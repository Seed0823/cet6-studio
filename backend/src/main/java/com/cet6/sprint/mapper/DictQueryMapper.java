package com.cet6.sprint.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cet6.sprint.entity.DictQuery;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 查词记录 Mapper
 */
public interface DictQueryMapper extends BaseMapper<DictQuery> {

    /**
     * 核心：查词计数 upsert
     * <p>
     * 靠唯一键 (user_id, word) 触发 ON DUPLICATE KEY UPDATE，
     * 一条 SQL 完成「有则 +1、无则插入」，避免 先查→再插/再改 的三次往返，
     * 且并发下由数据库保证原子性。
     *
     * @return 受影响行数（插入=1，更新=2）
     */
    @Insert("INSERT INTO t_dict_query (user_id, word, query_count) VALUES (#{userId}, #{word}, 1) "
            + "ON DUPLICATE KEY UPDATE query_count = query_count + 1, last_time = NOW()")
    int upsertIncrement(@Param("userId") Long userId, @Param("word") String word);

    /** 热词榜：按查询次数倒序 */
    @Select("SELECT * FROM t_dict_query WHERE user_id = #{userId} "
            + "ORDER BY query_count DESC, last_time DESC LIMIT #{limit}")
    List<DictQuery> hotList(@Param("userId") Long userId, @Param("limit") int limit);

    /** 最近查询 */
    @Select("SELECT * FROM t_dict_query WHERE user_id = #{userId} "
            + "ORDER BY last_time DESC LIMIT #{limit}")
    List<DictQuery> recentList(@Param("userId") Long userId, @Param("limit") int limit);

    /** 累计查词次数（去重后的词数 / 总次数分开统计） */
    @Select("SELECT IFNULL(SUM(query_count), 0) FROM t_dict_query WHERE user_id = #{userId}")
    int totalLookupTimes(@Param("userId") Long userId);

    @Select("SELECT COUNT(*) FROM t_dict_query WHERE user_id = #{userId}")
    int distinctWordCount(@Param("userId") Long userId);

    /** 查询某个词的历史次数 */
    @Select("SELECT IFNULL(query_count, 0) FROM t_dict_query WHERE user_id = #{userId} AND word = #{word}")
    Integer countOf(@Param("userId") Long userId, @Param("word") String word);
}
