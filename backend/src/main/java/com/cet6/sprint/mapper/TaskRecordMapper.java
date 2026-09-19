package com.cet6.sprint.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cet6.sprint.entity.TaskRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/**
 * 每日任务 Mapper
 */
public interface TaskRecordMapper extends BaseMapper<TaskRecord> {

    /**
     * 初始化当日任务（已存在则忽略，保证幂等）
     */
    @Insert("INSERT IGNORE INTO t_task_record "
            + "(user_id, task_date, task_key, task_name, target, finished, status) "
            + "VALUES (#{userId}, #{date}, #{taskKey}, #{taskName}, #{target}, 0, 0)")
    int initTask(@Param("userId") Long userId,
                 @Param("date") LocalDate date,
                 @Param("taskKey") String taskKey,
                 @Param("taskName") String taskName,
                 @Param("target") Integer target);

    /**
     * 推进任务进度
     * <p>
     * 用 GREATEST 保证进度只增不减（并发 / 重复提交时不会被改小）；
     * 达到目标自动置为已完成。
     */
    @Insert("INSERT INTO t_task_record "
            + "(user_id, task_date, task_key, task_name, target, finished, status) "
            + "VALUES (#{userId}, #{date}, #{taskKey}, #{taskName}, #{target}, #{finished}, "
            + "        IF(#{finished} >= #{target}, 1, 0)) "
            + "ON DUPLICATE KEY UPDATE "
            + "  target = GREATEST(target, #{target}), "
            + "  finished = GREATEST(finished, #{finished}), "
            + "  status = IF(GREATEST(finished, #{finished}) >= GREATEST(target, #{target}), 1, status)")
    int upsertProgress(@Param("userId") Long userId,
                       @Param("date") LocalDate date,
                       @Param("taskKey") String taskKey,
                       @Param("taskName") String taskName,
                       @Param("target") Integer target,
                       @Param("finished") Integer finished);

    @Select("SELECT * FROM t_task_record WHERE user_id = #{userId} AND task_date = #{date} ORDER BY id ASC")
    List<TaskRecord> listOfDay(@Param("userId") Long userId, @Param("date") LocalDate date);
}
