package com.cet6.sprint.dto;

import lombok.Data;

/**
 * 上报学习时长
 */
@Data
public class StudyLogDTO {

    /** 模块：word 背单词 / quiz 自测 / dict 查词 */
    private String module;

    private Integer durationMin;

    private Integer wordCount;
}
