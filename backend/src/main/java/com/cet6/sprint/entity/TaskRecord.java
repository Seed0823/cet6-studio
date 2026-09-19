package com.cet6.sprint.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 每日任务
 */
@Data
@TableName("t_task_record")
public class TaskRecord {

    /** 任务标识常量 */
    public static final String KEY_WORD = "word";
    public static final String KEY_REVIEW = "review";
    public static final String KEY_QUIZ = "quiz";
    public static final String KEY_DICT = "dict";

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate taskDate;

    private String taskKey;

    private String taskName;

    private Integer target;

    private Integer finished;

    /** 0 未完成 / 1 已完成 */
    private Integer status;

    private LocalDateTime updateTime;
}
