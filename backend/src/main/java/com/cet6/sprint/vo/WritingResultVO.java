package com.cet6.sprint.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 写作提交结果（字数校验 + 关键词覆盖率自测 + 参考范文）
 */
@Data
public class WritingResultVO {

    private Long id;
    private Integer wordCount;
    private Boolean meetsLength;
    private Integer minWords;
    private Integer maxWords;
    private String referenceEssay;
    private List<String> keywords;
    private Integer totalKeywords;
    private List<String> matchedKeywords;
    /** 关键词覆盖率 %，参考自测 */
    private BigDecimal coverage;
}
