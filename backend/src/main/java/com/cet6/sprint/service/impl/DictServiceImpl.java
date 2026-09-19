package com.cet6.sprint.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cet6.sprint.common.BusinessException;
import com.cet6.sprint.common.UserContext;
import com.cet6.sprint.entity.DictQuery;
import com.cet6.sprint.entity.Word;
import com.cet6.sprint.entity.WrongWord;
import com.cet6.sprint.mapper.DictQueryMapper;
import com.cet6.sprint.mapper.WordMapper;
import com.cet6.sprint.mapper.WrongWordMapper;
import com.cet6.sprint.service.DictService;
import com.cet6.sprint.service.ExternalSourceService;
import com.cet6.sprint.service.StudyService;
import com.cet6.sprint.vo.ExternalDictVO;
import com.cet6.sprint.vo.WordLookupVO;
import com.cet6.sprint.vo.WordSuggestVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * 查词服务实现
 */
@Service
@RequiredArgsConstructor
public class DictServiceImpl implements DictService {

    /** 前缀只取开头的连续单词字符，最长 30 —— 防止超长参数打满词库扫描 */
    private static final Pattern PREFIX_PATTERN = Pattern.compile("^[a-z][a-z'\\-]{0,29}");

    private static final int BRIEF_MAX = 50;

    private final WordMapper wordMapper;
    private final DictQueryMapper dictQueryMapper;
    private final WrongWordMapper wrongWordMapper;
    private final StudyService studyService;
    private final ExternalSourceService externalSourceService;

    @Override
    public WordLookupVO lookup(String raw) {
        Long userId = UserContext.getUserId();
        String key = normalize(raw);
        if (key.isEmpty()) {
            throw new BusinessException("请输入要查询的单词");
        }

        Word word = findByExact(key);
        if (word == null) {
            // 退化策略：先做简单词形还原（studies → study），再前缀模糊匹配
            String lemma = simpleLemma(key);
            if (!lemma.equals(key)) {
                word = findByExact(lemma);
            }
        }

        WordLookupVO vo = new WordLookupVO();
        if (word == null) {
            vo.setWord(key);
            vo.setExactMatch(false);
            vo.setSuggestions(suggest(key));
            // 本地词库未收录时仍然问一次在线源：至少让用户拿到音标和英文释义，
            // 比只看到「未收录」有用。在线源不可用会返回 null，不影响主流程。
            vo.setExternal(externalSourceService.lookupDict(key));
            return vo;
        }

        vo.setWordId(word.getId());
        vo.setWord(word.getWord());
        vo.setPhonetic(word.getPhonetic());
        vo.setPos(word.getPos());
        vo.setDefinition(word.getDefinition());
        vo.setTranslation(word.getTranslation());
        vo.setTag(word.getTag());
        vo.setExchange(word.getExchange());
        vo.setCet6(word.getIsCet6() != null && word.getIsCet6() == 1);
        vo.setExactMatch(true);

        // 外部源增强（音标 / 词性 / 英文释义 / 发音直链）。失败一律静默降级，
        // 本地词库的内容照常返回 —— 在线源是加分项，不能变成单点故障。
        enrichFromExternal(vo);

        // === 关键：查词计数 upsert，前端热词榜的数据来源 ===
        dictQueryMapper.upsertIncrement(userId, word.getWord());
        Integer count = dictQueryMapper.countOf(userId, word.getWord());
        vo.setQueryCount(count == null ? 1 : count);

        // 是否已在错词本
        Long inWrong = wrongWordMapper.selectCount(Wrappers.<WrongWord>lambdaQuery()
                .eq(WrongWord::getUserId, userId)
                .eq(WrongWord::getWordId, word.getId()));
        vo.setInWrongBook(inWrong != null && inWrong > 0);

        // 计入学习日志 + 推进「查词积累」每日任务
        studyService.record(userId, "dict", 0, 1);
        return vo;
    }

    @Override
    public List<WordSuggestVO> suggest(String raw, int limit) {
        String prefix = normalizePrefix(raw);
        // 一个字母都不给就直接返回空：不查库，也不给「最近查询」掺假数据（那是前端的活）
        if (prefix.isEmpty()) {
            return Collections.emptyList();
        }
        int size = Math.min(Math.max(limit, 1), 20);
        return wordMapper.suggestByPrefix(prefix + "%", size)
                .stream()
                .map(this::toSuggestVO)
                .collect(Collectors.toList());
    }

    /**
     * 前缀归一化：去空白、转小写，只保留开头连续的单词字符。
     * <p>
     * 这样 "Alle " → "alle"、"all eviate" → "all"、"1234" → ""（直接短路，不打库）。
     */
    private String normalizePrefix(String raw) {
        if (raw == null) {
            return "";
        }
        Matcher m = PREFIX_PATTERN.matcher(raw.trim().toLowerCase());
        return m.find() ? m.group() : "";
    }

    /** 词库实体 → 轻量候选对象：只取中文释义的首条义项 */
    private WordSuggestVO toSuggestVO(Word word) {
        WordSuggestVO vo = new WordSuggestVO();
        vo.setWordId(word.getId());
        vo.setWord(word.getWord());
        vo.setPhonetic(word.getPhonetic());
        vo.setBrief(brief(word.getTranslation()));
        vo.setCet6(word.getIsCet6() != null && word.getIsCet6() == 1);
        return vo;
    }

    /** 首条义项摘要，超长截断 */
    private String brief(String translation) {
        if (translation == null || translation.isBlank()) {
            return "";
        }
        String first = translation.split("\\r?\\n")[0].trim();
        return first.length() > BRIEF_MAX ? first.substring(0, BRIEF_MAX) + "…" : first;
    }

    @Override
    public List<DictQuery> hot(int limit) {
        return dictQueryMapper.hotList(UserContext.getUserId(), Math.min(limit, 200));
    }

    @Override
    public List<DictQuery> recent(int limit) {
        return dictQueryMapper.recentList(UserContext.getUserId(), Math.min(limit, 200));
    }

    @Override
    public Map<String, Object> stats() {
        Long userId = UserContext.getUserId();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("distinctWords", dictQueryMapper.distinctWordCount(userId));
        map.put("totalTimes", dictQueryMapper.totalLookupTimes(userId));
        List<DictQuery> top = dictQueryMapper.hotList(userId, 1);
        map.put("topWord", top.isEmpty() ? null : top.get(0).getWord());
        map.put("topWordCount", top.isEmpty() ? 0 : top.get(0).getQueryCount());
        return map;
    }

    @Override
    public void addToWrongBook(Long wordId) {
        Long userId = UserContext.getUserId();
        if (wordId == null || wordMapper.selectById(wordId) == null) {
            throw new BusinessException("单词不存在");
        }
        wrongWordMapper.upsertWrong(userId, wordId, "dict");
    }

    /**
     * 用外部源补全查词结果
     * <p>
     * 词典源与发音源是两个独立开关，所以四种组合都要能跑：
     * 两者都关 → 直接返回；只有发音开 → 也给出一个只带音频的对象。
     * 音标优先用在线源的 IPA，本地 ECDICT 的旧式记法仅作兜底。
     */
    private void enrichFromExternal(WordLookupVO vo) {
        ExternalDictVO ext = externalSourceService.lookupDict(vo.getWord());
        String audio = externalSourceService.pronUrl(vo.getWord());
        if (ext == null && audio == null) {
            return;
        }
        if (ext == null) {
            ext = new ExternalDictVO();
        }
        if (audio != null) {
            ext.setAudioUrl(audio);
            ext.setPronSource(externalSourceService.pronSource());
        }
        vo.setExternal(ext);

        // 音标优先用在线源的 IPA（/həˈloʊ/）。本地 ECDICT 用的是旧式 ASCII 记法
        // （hә'lәu、ri'ziliәns），可读性差、也不通用；在线源取不到时才回退本地。
        if (ext.getPhonetic() != null && !ext.getPhonetic().isBlank()) {
            vo.setPhonetic(ext.getPhonetic());
        }
    }

    private Word findByExact(String word) {
        return wordMapper.selectOne(Wrappers.<Word>lambdaQuery()
                .eq(Word::getWord, word)
                .last("LIMIT 1"));
    }

    /** 归一化：去空白、转小写、去掉首尾标点 */
    private String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.trim().toLowerCase()
                .replaceAll("^[^a-z]+", "")
                .replaceAll("[^a-z'\\-]+$", "");
    }

    /**
     * 极简词形还原（不引第三方 NLP 库，覆盖最常见几种后缀）
     * <p>
     * 更严谨的做法是接入 ECDICT 的 lemma 表或使用 Snowball 词干算法。
     */
    private String simpleLemma(String word) {
        if (word.length() <= 4) {
            return word;
        }
        if (word.endsWith("ies") && word.length() > 4) {
            return word.substring(0, word.length() - 3) + "y";
        }
        if (word.endsWith("es") && word.length() > 4) {
            return word.substring(0, word.length() - 2);
        }
        if (word.endsWith("s") && !word.endsWith("ss")) {
            return word.substring(0, word.length() - 1);
        }
        if (word.endsWith("ing") && word.length() > 5) {
            return word.substring(0, word.length() - 3);
        }
        if (word.endsWith("ed") && word.length() > 4) {
            return word.substring(0, word.length() - 2);
        }
        if (word.endsWith("ly") && word.length() > 5) {
            return word.substring(0, word.length() - 2);
        }
        return word;
    }

    /** 前缀模糊建议 */
    private List<String> suggest(String key) {
        String prefix = key.length() >= 3 ? key.substring(0, 3) : key;
        return wordMapper.selectList(Wrappers.<Word>lambdaQuery()
                        .likeRight(Word::getWord, prefix)
                        .orderByAsc(Word::getFrq)
                        .last("LIMIT 8"))
                .stream()
                .map(Word::getWord)
                .collect(Collectors.toList());
    }
}
