package com.cet6.sprint.vo;

import lombok.Data;

import java.util.List;

/**
 * 听力题目（不含正确答案，答案在后端判分）
 */
@Data
public class ListeningQuestionVO {

    private Integer index;
    private Long questionId;
    private String question;
    private List<String> options;
}
