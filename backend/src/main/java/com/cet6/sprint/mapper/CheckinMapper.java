package com.cet6.sprint.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cet6.sprint.entity.Checkin;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 打卡 Mapper
 */
public interface CheckinMapper extends BaseMapper<Checkin> {

    /** 打卡（INSERT IGNORE：同一天重复打卡不会报错，也不会产生重复行） */
    @Insert("INSERT IGNORE INTO t_checkin (user_id, checkin_date) VALUES (#{userId}, #{date})")
    int checkin(@Param("userId") Long userId, @Param("date") LocalDate date);

    /** 取最近的打卡日期（倒序），用于计算连续天数 */
    @Select("SELECT checkin_date FROM t_checkin WHERE user_id = #{userId} "
            + "ORDER BY checkin_date DESC LIMIT 180")
    List<LocalDate> recentDates(@Param("userId") Long userId);

    /** 累计打卡天数 */
    @Select("SELECT COUNT(*) FROM t_checkin WHERE user_id = #{userId}")
    int totalDays(@Param("userId") Long userId);
}
