package com.cet6.sprint.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 错词本
 */
@Data
@TableName("t_wrong_word")
public class WrongWord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long wordId;

    /** 来源：quiz 自测 / exam 模考 / dict 查词 */
    private String source;

    private Integer wrongCount;

    /** 0 未攻克 / 1 已攻克 */
    private Integer mastered;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
