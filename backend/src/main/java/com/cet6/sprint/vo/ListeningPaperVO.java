package com.cet6.sprint.vo;

import lombok.Data;

import java.util.List;

/**
 * 听力试卷（原文 + 题目，答案不下发）
 */
@Data
public class ListeningPaperVO {

    private Long id;
    private String title;
    private String category;
    private Integer difficulty;
    private String script;
    private List<ListeningQuestionVO> questions;
}
