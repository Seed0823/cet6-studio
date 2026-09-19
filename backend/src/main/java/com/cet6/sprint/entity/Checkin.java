package com.cet6.sprint.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

/**
 * 打卡记录
 * <p>
 * 连续打卡天数不落库，由 checkin_date 序列实时计算（避免状态字段不一致）。
 */
@Data
@TableName("t_checkin")
public class Checkin {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private LocalDate checkinDate;
}
