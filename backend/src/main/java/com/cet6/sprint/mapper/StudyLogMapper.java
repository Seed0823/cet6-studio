package com.cet6.sprint.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cet6.sprint.entity.StudyLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 学习日志 Mapper
 */
public interface StudyLogMapper extends BaseMapper<StudyLog> {

    /**
     * 累加当日某模块的学习数据（同一用户+日期+模块只保留一行）
     */
    @Insert("INSERT INTO t_study_log (user_id, study_date, module, duration_min, word_count) "
            + "VALUES (#{userId}, #{date}, #{module}, #{durationMin}, #{wordCount}) "
            + "ON DUPLICATE KEY UPDATE duration_min = duration_min + #{durationMin}, "
            + "word_count = word_count + #{wordCount}")
    int accumulate(@Param("userId") Long userId,
                   @Param("date") LocalDate date,
                   @Param("module") String module,
                   @Param("durationMin") Integer durationMin,
                   @Param("wordCount") Integer wordCount);

    /** 按日期区间取学习日志（统计页趋势） */
    @Select("SELECT * FROM t_study_log WHERE user_id = #{userId} AND study_date >= #{from} "
            + "ORDER BY study_date ASC")
    List<StudyLog> range(@Param("userId") Long userId, @Param("from") LocalDate from);

    /** 指定日期总学习时长（分钟） */
    @Select("SELECT IFNULL(SUM(duration_min), 0) FROM t_study_log WHERE user_id = #{userId} AND study_date = #{date}")
    int minutesOfDay(@Param("userId") Long userId, @Param("date") LocalDate date);

    /** 指定日期某模块的累计数量（词数 / 查词次数） */
    @Select("SELECT IFNULL(SUM(word_count), 0) FROM t_study_log "
            + "WHERE user_id = #{userId} AND study_date = #{date} AND module = #{module}")
    int countOfDay(@Param("userId") Long userId,
                   @Param("date") LocalDate date,
                   @Param("module") String module);

    /** 累计总学习时长 */
    @Select("SELECT IFNULL(SUM(duration_min), 0) FROM t_study_log WHERE user_id = #{userId}")
    int totalMinutes(@Param("userId") Long userId);

    /**
     * 某模块的历史累计学习分钟数（跨天）
     * <p>
     * 用于阅读时长的结转：阅读记录以「秒」累计，而学习日志以「分钟」记账，
     * 每次进度上报只有几秒，逐次除 60 永远是 0。这里取该模块已记账的分钟数，
     * 与「阅读总秒数 / 60」求差，只补记差额 —— 这样既不会漏记，也不会重复记。
     */
    @Select("SELECT IFNULL(SUM(duration_min), 0) FROM t_study_log "
            + "WHERE user_id = #{userId} AND module = #{module}")
    int totalMinutesOfModule(@Param("userId") Long userId, @Param("module") String module);
}
