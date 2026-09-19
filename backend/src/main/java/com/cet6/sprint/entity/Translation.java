package com.cet6.sprint.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 翻译练习（中译英）
 */
@Data
@TableName("t_translation")
public class Translation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String category;
    private String title;
    private String chineseText;
    private String referenceEn;
    private String keyPoints;
    private Integer difficulty;
    private LocalDateTime createTime;
}
