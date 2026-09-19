package com.cet6.sprint.service.impl;

import com.cet6.sprint.common.BusinessException;
import com.cet6.sprint.common.UserContext;
import com.cet6.sprint.dto.WritingSubmitDTO;
import com.cet6.sprint.entity.Writing;
import com.cet6.sprint.mapper.WritingMapper;
import com.cet6.sprint.service.StudyService;
import com.cet6.sprint.service.WritingService;
import com.cet6.sprint.vo.WritingItemVO;
import com.cet6.sprint.vo.WritingResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 写作练习服务实现
 * <p>
 * 字数校验走真实分词计数；关键词覆盖率同样是参考自测（无 LLM 评分）。
 */
@Service
@RequiredArgsConstructor
public class WritingServiceImpl implements WritingService {

    private final WritingMapper writingMapper;
    private final StudyService studyService;

    @Override
    public List<WritingItemVO> random(int count) {
        int n = Math.max(1, Math.min(count, 10));
        List<Writing> list = writingMapper.random(n);
        if (list.isEmpty()) {
            throw new BusinessException("题库暂无写作题目");
        }
        return list.stream().map(w -> {
            WritingItemVO vo = new WritingItemVO();
            vo.setId(w.getId());
            vo.setCategory(w.getCategory());
            vo.setPrompt(w.getPrompt());
            vo.setRequirement(w.getRequirement());
            vo.setMinWords(w.getMinWords());
            vo.setMaxWords(w.getMaxWords());
            vo.setOutlinePoints(parseLines(w.getOutlinePoints()));
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public WritingResultVO submit(WritingSubmitDTO dto) {
        Long userId = UserContext.getUserId();
        Writing w = writingMapper.selectById(dto.getId());
        if (w == null) {
            throw new BusinessException("题目不存在");
        }
        String answer = dto.getAnswer() == null ? "" : dto.getAnswer();
        int wordCount = answer.trim().isEmpty() ? 0 : answer.trim().split("\\s+").length;
        int min = w.getMinWords() == null ? 0 : w.getMinWords();
        boolean meetsLength = wordCount >= min;

        List<String> keys = parsePoints(w.getKeywords());
        List<String> matched = new ArrayList<>();
        String lower = answer.toLowerCase();
        for (String k : keys) {
            String kk = k.toLowerCase().trim();
            if (!kk.isEmpty() && lower.contains(kk)) {
                matched.add(k);
            }
        }
        int total = keys.size();
        BigDecimal coverage = total == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(matched.size() * 100.0 / total).setScale(0, RoundingMode.HALF_UP);

        WritingResultVO vo = new WritingResultVO();
        vo.setId(w.getId());
        vo.setWordCount(wordCount);
        vo.setMeetsLength(meetsLength);
        vo.setMinWords(min);
        vo.setMaxWords(w.getMaxWords());
        vo.setReferenceEssay(w.getReferenceEssay());
        vo.setKeywords(keys);
        vo.setTotalKeywords(total);
        vo.setMatchedKeywords(matched);
        vo.setCoverage(coverage);

        studyService.record(userId, "writing", 0, 1);
        studyService.checkinToday(userId);
        return vo;
    }

    private List<String> parseLines(String s) {
        if (s == null || s.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(s.split("\\R"))
                .map(String::trim)
                .filter(x -> !x.isEmpty())
                .collect(Collectors.toList());
    }

    private List<String> parsePoints(String s) {
        if (s == null || s.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(s.split("[;；,]"))
                .map(String::trim)
                .filter(x -> !x.isEmpty())
                .collect(Collectors.toList());
    }
}
