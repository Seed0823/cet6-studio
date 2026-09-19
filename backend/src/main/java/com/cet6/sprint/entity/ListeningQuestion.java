package com.cet6.sprint.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 听力选择题
 */
@Data
@TableName("t_listening_question")
public class ListeningQuestion {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long listeningId;
    private String question;
    private String optionsJson;
    private Integer answerIndex;
    private String explain;
}
