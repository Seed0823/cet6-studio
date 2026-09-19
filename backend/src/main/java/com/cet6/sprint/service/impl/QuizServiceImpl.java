package com.cet6.sprint.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cet6.sprint.common.BusinessException;
import com.cet6.sprint.common.UserContext;
import com.cet6.sprint.dto.QuizSubmitDTO;
import com.cet6.sprint.entity.QuizDetail;
import com.cet6.sprint.entity.QuizRecord;
import com.cet6.sprint.entity.Word;
import com.cet6.sprint.mapper.QuizDetailMapper;
import com.cet6.sprint.mapper.QuizRecordMapper;
import com.cet6.sprint.mapper.WordMapper;
import com.cet6.sprint.mapper.WrongWordMapper;
import com.cet6.sprint.service.QuizService;
import com.cet6.sprint.service.StudyService;
import com.cet6.sprint.vo.QuizQuestionVO;
import com.cet6.sprint.vo.QuizResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 单词自测服务实现
 */
@Service
@RequiredArgsConstructor
public class QuizServiceImpl implements QuizService {

    private static final int OPTION_COUNT = 4;

    private final WordMapper wordMapper;
    private final QuizRecordMapper quizRecordMapper;
    private final QuizDetailMapper quizDetailMapper;
    private final WrongWordMapper wrongWordMapper;
    private final StudyService studyService;

    @Override
    public List<QuizQuestionVO> generate(int count) {
        Long userId = UserContext.getUserId();
        int n = Math.max(1, Math.min(count, 30));

        // 优先取没考过的词；不够则用其他六级词补齐
        List<Word> words = new ArrayList<>(wordMapper.randomNewCet6(userId, n));
        if (words.size() < n) {
            Set<Long> ids = new HashSet<>();
            words.forEach(w -> ids.add(w.getId()));
            for (Word w : wordMapper.randomCet6(n * 3)) {
                if (ids.add(w.getId())) {
                    words.add(w);
                }
                if (words.size() >= n) {
                    break;
                }
            }
        }

        List<QuizQuestionVO> questions = new ArrayList<>();
        int index = 1;
        for (Word w : words) {
            String correct = firstMeaning(w.getTranslation());
            if (correct.isBlank()) {
                continue;
            }
            // 干扰项：随机取其他词的中文释义，去重后凑够 4 个
            List<String> options = new ArrayList<>();
            options.add(correct);
            for (Word d : wordMapper.randomDistractors(w.getId(), 20)) {
                String meaning = firstMeaning(d.getTranslation());
                if (meaning.isBlank() || options.contains(meaning)) {
                    continue;
                }
                options.add(meaning);
                if (options.size() >= OPTION_COUNT) {
                    break;
                }
            }
            if (options.size() < OPTION_COUNT) {
                // 干扰项不足的题直接跳过，避免出现「三选一」这种送分题
                continue;
            }
            Collections.shuffle(options);

            QuizQuestionVO q = new QuizQuestionVO();
            q.setIndex(index++);
            q.setWordId(w.getId());
            q.setQuestion("下列哪一项是 " + w.getWord() + " 的中文释义？");
            q.setOptions(options);
            questions.add(q);
        }
        if (questions.isEmpty()) {
            throw new BusinessException("题库暂时不可用，请稍后重试");
        }
        return questions;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public QuizResultVO submit(QuizSubmitDTO dto) {
        Long userId = UserContext.getUserId();
        List<QuizSubmitDTO.Item> answers = dto.getAnswers();
        if (answers == null || answers.isEmpty()) {
            throw new BusinessException("答卷不能为空");
        }

        List<QuizResultVO.Item> details = new ArrayList<>();
        int correctCount = 0;

        for (QuizSubmitDTO.Item it : answers) {
            Word w = it.getWordId() == null ? null : wordMapper.selectById(it.getWordId());
            String correctMeaning = w == null ? null : firstMeaning(w.getTranslation());
            List<String> options = it.getOptions() == null ? Collections.emptyList() : it.getOptions();
            int correctIndex = correctMeaning == null ? -1 : options.indexOf(correctMeaning);
            int chosenIndex = it.getChosenIndex() == null ? -1 : it.getChosenIndex();
            boolean right = correctIndex >= 0 && correctIndex == chosenIndex;
            if (right) {
                correctCount++;
            } else if (w != null) {
                // 答错即入错词本（已存在则次数 +1）
                wrongWordMapper.upsertWrong(userId, w.getId(), "quiz");
            }

            QuizResultVO.Item item = new QuizResultVO.Item();
            item.setWordId(it.getWordId());
            item.setQuestion(it.getQuestion());
            item.setOptions(options);
            item.setChosenIndex(chosenIndex);
            item.setCorrectIndex(correctIndex);
            item.setCorrectTranslation(correctMeaning);
            item.setRight(right);
            if (w != null) {
                item.setWord(w.getWord());
                item.setPhonetic(w.getPhonetic());
            }
            details.add(item);
        }

        int total = details.size();
        BigDecimal accuracy = BigDecimal.valueOf(total == 0 ? 0 : correctCount * 100.0 / total)
                .setScale(2, RoundingMode.HALF_UP);

        // 落库：一条汇总记录 + N 条明细
        QuizRecord record = new QuizRecord();
        record.setUserId(userId);
        record.setTotal(total);
        record.setCorrect(correctCount);
        record.setAccuracy(accuracy);
        record.setCostSecond(dto.getCostSecond() == null ? 0 : dto.getCostSecond());
        record.setCreateTime(LocalDateTime.now());
        quizRecordMapper.insert(record);

        for (QuizResultVO.Item item : details) {
            QuizDetail d = new QuizDetail();
            d.setRecordId(record.getId());
            d.setWordId(item.getWordId());
            d.setQuestion(item.getQuestion());
            d.setOptionsJson(item.getOptions() == null ? null : String.join("\u0001", item.getOptions()));
            d.setCorrectIndex(item.getCorrectIndex());
            d.setChosenIndex(item.getChosenIndex());
            d.setIsRight(item.isRight() ? 1 : 0);
            quizDetailMapper.insert(d);
        }

        studyService.record(userId, "quiz", 0, total);
        studyService.checkinToday(userId);

        QuizResultVO vo = new QuizResultVO();
        vo.setRecordId(record.getId());
        vo.setTotal(total);
        vo.setCorrect(correctCount);
        vo.setAccuracy(accuracy);
        vo.setCostSecond(record.getCostSecond());
        vo.setDetails(details);
        return vo;
    }

    @Override
    public List<QuizRecord> history(int limit) {
        return quizRecordMapper.selectList(Wrappers.<QuizRecord>lambdaQuery()
                .eq(QuizRecord::getUserId, UserContext.getUserId())
                .orderByDesc(QuizRecord::getCreateTime)
                .last("LIMIT " + Math.min(limit, 50)));
    }

    /**
     * 取第一个义项（同时用于出题和判分，必须保持完全一致）
     */
    private String firstMeaning(String translation) {
        if (translation == null || translation.isBlank()) {
            return "";
        }
        String first = translation.split("\\R")[0].trim();
        if (first.length() > 40) {
            first = first.substring(0, 40);
        }
        return first;
    }
}
