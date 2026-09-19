package com.cet6.sprint.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cet6.sprint.common.UserContext;
import com.cet6.sprint.dto.WordGradeDTO;
import com.cet6.sprint.entity.Word;
import com.cet6.sprint.entity.WordGrade;
import com.cet6.sprint.mapper.WordGradeMapper;
import com.cet6.sprint.mapper.WordMapper;
import com.cet6.sprint.service.StudyService;
import com.cet6.sprint.service.WordService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 背单词服务实现
 */
@Service
@RequiredArgsConstructor
public class WordServiceImpl implements WordService {

    private final WordMapper wordMapper;
    private final WordGradeMapper wordGradeMapper;
    private final StudyService studyService;

    @Override
    public List<Word> todayWords(int limit) {
        Long userId = UserContext.getUserId();
        // 优先推没学过的词
        List<Word> list = wordMapper.randomNewCet6(userId, limit);
        if (list.isEmpty()) {
            // 5400 词全部学过之后，转为随机复习模式
            list = wordMapper.randomCet6(limit);
        }
        return list;
    }

    @Override
    public List<Word> reviewWords(int limit) {
        return wordMapper.findDueReview(UserContext.getUserId(), limit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitGrade(WordGradeDTO dto) {
        Long userId = UserContext.getUserId();

        // 取出历史复习次数，用于推算下次复习间隔
        WordGrade exist = wordGradeMapper.selectOne(Wrappers.<WordGrade>lambdaQuery()
                .eq(WordGrade::getUserId, userId)
                .eq(WordGrade::getWordId, dto.getWordId()));
        int reviewCount = (exist == null || exist.getReviewCount() == null) ? 0 : exist.getReviewCount();

        wordGradeMapper.upsertGrade(userId, dto.getWordId(), dto.getGrade(),
                nextReview(dto.getGrade(), reviewCount));

        boolean fromReview = Boolean.TRUE.equals(dto.getFromReview());
        studyService.record(userId, fromReview ? "review" : "word",
                dto.getDurationMin() == null ? 0 : dto.getDurationMin(), 1);
        // 有学习行为即视为当天已打卡
        studyService.checkinToday(userId);
    }

    @Override
    public Page<Word> page(int pageNo, int size, String keyword, Integer cet6Only) {
        var qw = Wrappers.<Word>lambdaQuery();
        if (StringUtils.hasText(keyword)) {
            qw.and(w -> w.like(Word::getWord, keyword).or().like(Word::getTranslation, keyword));
        }
        if (cet6Only != null && cet6Only == 1) {
            qw.eq(Word::getIsCet6, 1);
        }
        qw.orderByAsc(Word::getFrq);
        return wordMapper.selectPage(new Page<>(pageNo, Math.min(size, 100)), qw);
    }

    @Override
    public Map<String, Integer> progress() {
        Long userId = UserContext.getUserId();
        Map<String, Integer> map = new LinkedHashMap<>();
        map.put("learned", wordGradeMapper.countLearned(userId));
        map.put("known", wordGradeMapper.countKnown(userId));
        map.put("dueReview", wordGradeMapper.countDueReview(userId));
        return map;
    }

    /**
     * 艾宾浩斯复习间隔
     * <p>
     * 不认识 → 明天重来；
     * 模糊   → 2 天后；
     * 认识   → 4 / 8 / 16 / 30 天递增（复习次数越多，间隔越长）。
     */
    private LocalDate nextReview(int grade, int reviewCount) {
        LocalDate today = LocalDate.now();
        if (grade == WordGrade.UNKNOWN) {
            return today.plusDays(1);
        }
        if (grade == WordGrade.VAGUE) {
            return today.plusDays(2);
        }
        long days = Math.min(4L << Math.min(reviewCount, 3), 30L);
        return today.plusDays(days);
    }
}
