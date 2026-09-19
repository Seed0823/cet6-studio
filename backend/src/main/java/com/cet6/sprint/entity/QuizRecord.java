package com.cet6.sprint.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 测验记录
 */
@Data
@TableName("t_quiz_record")
public class QuizRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Integer total;

    private Integer correct;

    private BigDecimal accuracy;

    private Integer costSecond;

    private LocalDateTime createTime;
}
