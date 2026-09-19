package com.cet6.sprint.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 查词记录
 * <p>
 * 唯一键 (user_id, word)，每次查询走 upsert 累加 query_count，
 * 热词榜直接按 query_count 倒序取。
 */
@Data
@TableName("t_dict_query")
public class DictQuery {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String word;

    /** 累计查询次数 */
    private Integer queryCount;

    private LocalDateTime firstTime;

    private LocalDateTime lastTime;
}
