package com.cet6.sprint.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 单词掌握度
 */
@Data
@TableName("t_word_grade")
public class WordGrade {

    /** 掌握等级常量 */
    public static final int UNKNOWN = 0;
    public static final int VAGUE = 1;
    public static final int KNOWN = 2;

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long wordId;

    /** 0 不认识 / 1 模糊 / 2 认识 */
    private Integer grade;

    private Integer reviewCount;

    /** 艾宾浩斯下次复习日期 */
    private LocalDate nextReview;

    private LocalDateTime updateTime;
}
