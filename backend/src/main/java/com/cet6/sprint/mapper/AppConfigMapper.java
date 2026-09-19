package com.cet6.sprint.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cet6.sprint.entity.AppConfig;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 应用配置 Mapper
 */
public interface AppConfigMapper extends BaseMapper<AppConfig> {

    /** 全量读取（配置项很少，直接一次捞出，服务内缓存） */
    @Select("SELECT * FROM t_app_config ORDER BY id")
    List<AppConfig> selectAllConfig();

    /**
     * 写入配置：靠唯一键 uk_cfg_key 走 upsert，一条 SQL 完成「有则改、无则插」。
     * <p>
     * cfg_value 允许写空串（空串 = 使用内置默认地址），所以不能靠 NULL 判断，
     * 这里显式把空串也写进去。
     */
    @Insert("INSERT INTO t_app_config (cfg_key, cfg_value) VALUES (#{key}, #{value}) "
            + "ON DUPLICATE KEY UPDATE cfg_value = VALUES(cfg_value)")
    int upsert(@Param("key") String key, @Param("value") String value);

    /** 读取单个配置值 */
    @Select("SELECT cfg_value FROM t_app_config WHERE cfg_key = #{key}")
    String selectValue(@Param("key") String key);
}
