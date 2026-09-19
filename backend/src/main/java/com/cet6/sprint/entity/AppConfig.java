package com.cet6.sprint.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 应用配置（key-value）
 * <p>
 * 全局配置，不区分用户。只存「覆盖值」——未写入的 key 由 SettingsServiceImpl
 * 用内置默认值兜底，因此新增配置项不必改表结构。
 */
@Data
@TableName("t_app_config")
public class AppConfig {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String cfgKey;

    private String cfgValue;

    private String remark;

    /** 由数据库 ON UPDATE 维护，这里只读 */
    @TableField(updateStrategy = FieldStrategy.NEVER)
    private LocalDateTime updateTime;
}
