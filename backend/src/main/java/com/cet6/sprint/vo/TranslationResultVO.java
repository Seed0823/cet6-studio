package com.cet6.sprint.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 翻译提交结果（含参考译文与关键点覆盖率自测）
 */
@Data
public class TranslationResultVO {

    private Long id;
    private String referenceEn;
    private List<String> keyPoints;
    private Integer totalPoints;
    private List<String> matchedPoints;
    /** 关键点覆盖率 %，仅供参考自测，非官方评分 */
    private BigDecimal coverage;

    /**
     * 机器参考译文（在线翻译源生成）
     * <p>
     * 与题库人工参考译文 referenceEn 互补：referenceEn 是「标准答案」，
     * machineTranslation 是「你这句中文的另一种译法」，用来对照自己的表达。
     * 在线源关闭或不可用时为 null。
     */
    private String machineTranslation;

    /** 机器译文来源引擎，如 mymemory */
    private String machineEngine;

    /** 机器译文说明（例如原文过长被分段、部分段落失败） */
    private String machineNote;
}
