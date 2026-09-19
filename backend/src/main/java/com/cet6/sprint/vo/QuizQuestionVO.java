package com.cet6.sprint.vo;

import lombok.Data;

import java.util.List;

/**
 * 测验题目（不含正确答案，答案在后端判分）
 */
@Data
public class QuizQuestionVO {

    /** 题号，从 1 开始 */
    private Integer index;

    private Long wordId;

    private String question;

    private List<String> options;
}
