package com.cet6.sprint.service.impl;

import com.cet6.sprint.common.BusinessException;
import com.cet6.sprint.common.UserContext;
import com.cet6.sprint.dto.TranslationSubmitDTO;
import com.cet6.sprint.entity.Translation;
import com.cet6.sprint.mapper.TranslationMapper;
import com.cet6.sprint.service.ExternalSourceService;
import com.cet6.sprint.service.StudyService;
import com.cet6.sprint.service.TranslationService;
import com.cet6.sprint.vo.TranslationItemVO;
import com.cet6.sprint.vo.TranslationResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 翻译练习服务实现
 * <p>
 * 没有 LLM 评分能力，采用「关键点覆盖率」作为参考自测：把 key_points 拆成若干关键英文
 * 词/短语，统计用户译文里命中了几个，给出覆盖率百分比。明确标注为参考自测，非官方评分。
 */
@Service
@RequiredArgsConstructor
public class TranslationServiceImpl implements TranslationService {

    private final TranslationMapper translationMapper;
    private final StudyService studyService;
    private final ExternalSourceService externalSourceService;

    @Override
    public List<TranslationItemVO> random(int count) {
        int n = Math.max(1, Math.min(count, 10));
        List<Translation> list = translationMapper.random(n);
        if (list.isEmpty()) {
            throw new BusinessException("题库暂无翻译题目");
        }
        return list.stream().map(t -> {
            TranslationItemVO vo = new TranslationItemVO();
            vo.setId(t.getId());
            vo.setCategory(t.getCategory());
            vo.setTitle(t.getTitle());
            vo.setChineseText(t.getChineseText());
            vo.setDifficulty(t.getDifficulty());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public TranslationResultVO submit(TranslationSubmitDTO dto) {
        Long userId = UserContext.getUserId();
        Translation t = translationMapper.selectById(dto.getId());
        if (t == null) {
            throw new BusinessException("题目不存在");
        }
        String answer = dto.getAnswer() == null ? "" : dto.getAnswer();
        List<String> points = parsePoints(t.getKeyPoints());
        List<String> matched = new ArrayList<>();
        String lower = answer.toLowerCase();
        for (String p : points) {
            String pp = p.toLowerCase().trim();
            if (!pp.isEmpty() && lower.contains(pp)) {
                matched.add(p);
            }
        }
        int total = points.size();
        BigDecimal coverage = total == 0 ? BigDecimal.ZERO
                : BigDecimal.valueOf(matched.size() * 100.0 / total).setScale(0, RoundingMode.HALF_UP);

        TranslationResultVO vo = new TranslationResultVO();
        vo.setId(t.getId());
        vo.setReferenceEn(t.getReferenceEn());
        vo.setKeyPoints(points);
        vo.setTotalPoints(total);
        vo.setMatchedPoints(matched);
        vo.setCoverage(coverage);

        // 机器参考译文：给「同一句中文的另一种译法」，与人工参考译文互为补充。
        // 在线源不可用时保持 null，前端只展示人工译文，不报错。
        attachMachineTranslation(t, vo);

        studyService.record(userId, "translation", 0, 1);
        studyService.checkinToday(userId);
        return vo;
    }

    /**
     * 调用在线翻译源，生成机器参考译文。
     * <p>
     * 捕获所有异常：翻译没有（或关掉）在线源时，翻译练习的核心功能
     * ——人工参考译文 + 关键点覆盖率——必须照常可用。
     */
    private void attachMachineTranslation(Translation t, TranslationResultVO vo) {
        try {
            ExternalSourceService.TranslateOutcome outcome =
                    externalSourceService.translateZh2En(t.getChineseText());
            if (outcome == null) {
                return;
            }
            vo.setMachineTranslation(outcome.getText());
            vo.setMachineEngine(outcome.getEngine());
            if (!outcome.isComplete()) {
                vo.setMachineNote("原文较长，已分 " + outcome.getSegments() + " 段翻译，其中部分段落未成功，仅供参考");
            } else if (outcome.getSegments() > 1) {
                vo.setMachineNote("原文较长，已分 " + outcome.getSegments() + " 段翻译后拼接");
            }
        } catch (Exception e) {
            // 在线源是加分项，失败不影响主流程
        }
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
