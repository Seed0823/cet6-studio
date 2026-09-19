package com.cet6.sprint.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/**
 * 学习日志（按天 + 模块聚合）
 */
@Data
@TableName("t_study_log")
public class StudyLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate studyDate;

    /** 模块：word 背单词 / quiz 自测 / dict 查词 */
    private String module;

    private Integer durationMin;

    private Integer wordCount;
}
