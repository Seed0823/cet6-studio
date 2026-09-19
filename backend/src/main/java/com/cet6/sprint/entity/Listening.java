package com.cet6.sprint.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 听力练习
 */
@Data
@TableName("t_listening")
public class Listening {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;
    private String category;
    private String script;
    private Integer difficulty;
    private LocalDateTime createTime;
}
