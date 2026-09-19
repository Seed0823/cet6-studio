package com.cet6.sprint.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cet6.sprint.common.BusinessException;
import com.cet6.sprint.common.UserContext;
import com.cet6.sprint.dto.ReadingProgressDTO;
import com.cet6.sprint.entity.Article;
import com.cet6.sprint.entity.ReadingRecord;
import com.cet6.sprint.mapper.ArticleMapper;
import com.cet6.sprint.mapper.ReadingRecordMapper;
import com.cet6.sprint.mapper.StudyLogMapper;
import com.cet6.sprint.mapper.WordMapper;
import com.cet6.sprint.service.ArticleService;
import com.cet6.sprint.vo.ArticleDetailVO;
import com.cet6.sprint.vo.ArticleListVO;
import com.cet6.sprint.vo.ReadingStatsVO;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 悦读服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ArticleServiceImpl implements ArticleService {

    private final ArticleMapper articleMapper;
    private final ReadingRecordMapper readingRecordMapper;
    private final WordMapper wordMapper;
    private final StudyLogMapper studyLogMapper;

    /** 进度达到该值即视为读完：滚到底是 100%，但用户常常差一点点就不再往下滚 */
    private static final int FINISH_THRESHOLD = 95;

    /** 学习日志里阅读模块的标识 */
    private static final String MODULE_READING = "reading";

    /** 单页上限，防止前端传个 999 */
    private static final int MAX_PAGE_SIZE = 50;

    /** 英文词切分：字母开头，中间允许连字符与撇号（well-known / don't） */
    private static final Pattern TOKEN = Pattern.compile("[A-Za-z][A-Za-z'-]*");

    /**
     * 六级大纲词表（小写拼写）
     * <p>
     * 启动时一次性载入内存，之后判断「某个词是不是六级词」就是一次 HashSet.contains。
     * 换成每篇文章去 JOIN 词库也能实现，但那是把一次 O(1) 的内存操作变成一次
     * 数据库往返；5000 多个短字符串的常驻内存代价不到 1 MB，换得完全划算。
     */
    private volatile Set<String> cet6Dict = Collections.emptySet();

    /** 文章 ID -> 正文中的六级词（顺序去重）。正文不变，缓存无需失效 */
    private final Map<Long, List<String>> cet6Cache = new ConcurrentHashMap<>();

    @PostConstruct
    public void loadCet6Dict() {
        try {
            List<String> words = wordMapper.listCet6Words();
            Set<String> set = new HashSet<>(Math.max(16, words.size() * 2));
            for (String w : words) {
                if (w != null && !w.isEmpty()) {
                    set.add(w.toLowerCase());
                }
            }
            this.cet6Dict = set;
            log.info("阅读模块：六级大纲词表已载入 {} 词", set.size());
        } catch (Exception e) {
            // 词表加载失败不应阻塞应用启动：退化成「不做六级词高亮」，其余功能可用
            log.warn("阅读模块：六级词表加载失败，六级词高亮将不可用", e);
        }
    }

    @Override
    public IPage<ArticleListVO> list(int page, int size, String category, Integer difficulty,
                                     String keyword, Boolean unfinishedOnly) {
        int current = Math.max(page, 1);
        int pageSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Page<ArticleListVO> p = new Page<>(current, pageSize);
        return articleMapper.pageList(p, UserContext.getUserId(), category, difficulty,
                keyword, unfinishedOnly);
    }

    @Override
    public ArticleDetailVO detail(Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw new BusinessException("文章不存在");
        }
        ArticleDetailVO vo = new ArticleDetailVO();
        BeanUtils.copyProperties(article, vo);
        vo.setCet6Words(cet6Cache.computeIfAbsent(articleId, k -> extractCet6(article.getContent())));

        ReadingRecord record = readingRecordMapper.find(UserContext.getUserId(), articleId);
        vo.setProgress(record == null || record.getProgress() == null ? 0 : record.getProgress());
        vo.setFinished(record == null || record.getFinished() == null ? 0 : record.getFinished());
        vo.setDurationSec(record == null || record.getDurationSec() == null ? 0 : record.getDurationSec());
        return vo;
    }

    @Override
    public void open(Long articleId) {
        if (articleMapper.selectById(articleId) == null) {
            throw new BusinessException("文章不存在");
        }
        readingRecordMapper.openOnce(UserContext.getUserId(), articleId);
    }

    @Override
    public void reportProgress(ReadingProgressDTO dto) {
        Long userId = UserContext.getUserId();
        int progress = dto.getProgress() == null ? 0 : dto.getProgress();
        int addSeconds = dto.getAddSeconds() == null ? 0 : dto.getAddSeconds();
        int finished = progress >= FINISH_THRESHOLD ? 1 : 0;
        readingRecordMapper.saveProgress(userId, dto.getArticleId(), progress, addSeconds, finished);
        carryOverToStudyLog(userId);
    }

    /**
     * 把阅读时长结转进学习日志（统计页的数据源）
     * <p>
     * 为什么不用「本次 addSeconds / 60」：前端每 10 秒上报一次，逐次除 60 永远是 0，
     * 学习日志上会一行都记不上，统计页的阅读时长恒为 0。
     * <p>
     * 改用「总量差值法」：阅读记录里存的是累计秒数，学习日志里存的是累计分钟数，
     * 两者取 floor 后求差，只补记差额。好处是幂等 —— 同样的请求重放十次，
     * 差额为 0 就不会重复记账；坏处是跨天时有不到 1 分钟的归属误差，可以接受。
     */
    private void carryOverToStudyLog(Long userId) {
        try {
            int shouldMinutes = readingRecordMapper.totalSeconds(userId) / 60;
            int loggedMinutes = studyLogMapper.totalMinutesOfModule(userId, MODULE_READING);
            int delta = shouldMinutes - loggedMinutes;
            if (delta > 0) {
                studyLogMapper.accumulate(userId, LocalDate.now(), MODULE_READING, delta, 0);
            }
        } catch (Exception e) {
            // 结转失败只是统计数字少一点，不该让进度上报整体失败
            log.warn("阅读时长结转学习日志失败, userId={}", userId, e);
        }
    }

    @Override
    public ReadingStatsVO stats() {
        Long userId = UserContext.getUserId();
        ReadingStatsVO vo = new ReadingStatsVO();
        vo.setTotal(articleMapper.totalCount());
        vo.setFinishedCount(articleMapper.finishedCount(userId));
        vo.setReadingCount(articleMapper.readingCount(userId));
        vo.setTotalMinutes(articleMapper.totalDurationSec(userId) / 60);

        List<ReadingStatsVO.CategoryStat> categories = new ArrayList<>();
        for (Map<String, Object> row : articleMapper.statsByCategory(userId)) {
            ReadingStatsVO.CategoryStat cs = new ReadingStatsVO.CategoryStat();
            cs.setCategory((String) row.get("category"));
            cs.setTotal(toInt(row.get("total")));
            cs.setFinished(toInt(row.get("finished")));
            categories.add(cs);
        }
        vo.setCategories(categories);

        // 「已精读到的六级词」= 读过的所有文章正文里出现过的六级词并集
        Set<String> words = new HashSet<>();
        for (String content : articleMapper.readContents(userId)) {
            words.addAll(extractCet6(content));
        }
        vo.setCet6WordCount(words.size());
        return vo;
    }

    /**
     * 从正文中提取六级大纲词（按首次出现顺序去重）
     */
    private List<String> extractCet6(String content) {
        Set<String> dict = this.cet6Dict;
        if (content == null || content.isEmpty() || dict.isEmpty()) {
            return Collections.emptyList();
        }
        Set<String> hit = new LinkedHashSet<>();
        Matcher m = TOKEN.matcher(content);
        while (m.find()) {
            String word = m.group().toLowerCase();
            if (dict.contains(word)) {
                hit.add(word);
            }
        }
        return new ArrayList<>(hit);
    }

    private int toInt(Object o) {
        return o instanceof Number n ? n.intValue() : 0;
    }
}
