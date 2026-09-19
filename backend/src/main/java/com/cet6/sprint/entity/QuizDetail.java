package com.cet6.sprint.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 测验明细（每题作答）
 */
@Data
@TableName("t_quiz_detail")
public class QuizDetail {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long recordId;

    private Long wordId;

    private String question;

    private String optionsJson;

    private Integer correctIndex;

    private Integer chosenIndex;

    private Integer isRight;
}
