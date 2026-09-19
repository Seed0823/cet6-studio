package com.cet6.sprint.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 写作练习（命题作文）
 */
@Data
@TableName("t_writing")
public class Writing {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String category;
    private String prompt;
    private String requirement;
    private String outlinePoints;
    private String referenceEssay;
    private String keywords;
    private Integer minWords;
    private Integer maxWords;
    private LocalDateTime createTime;
}
