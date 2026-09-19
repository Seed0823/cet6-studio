package com.cet6.sprint.vo;

import lombok.Data;

import java.util.List;

/**
 * 写作练习题目（练习态：含提纲提示，不含参考范文与关键词）
 */
@Data
public class WritingItemVO {

    private Long id;
    private String category;
    private String prompt;
    private String requirement;
    private Integer minWords;
    private Integer maxWords;
    private List<String> outlinePoints;
}
