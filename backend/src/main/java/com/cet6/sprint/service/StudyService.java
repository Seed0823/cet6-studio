package com.cet6.sprint.service;

import com.cet6.sprint.entity.TaskRecord;

import java.time.LocalDate;
import java.util.List;

/**
 * 学习记录服务
 * <p>
 * 收敛「学习日志累计 / 每日任务推进 / 打卡 / 连续天数」这些被多个业务模块
 * （背单词、自测、查词）共同依赖的操作，避免在每个 Service 里重复实现。
 */
public interface StudyService {

    /**
     * 记录一次学习行为
     * <p>
     * 一次调用完成三件事：写学习日志、刷新当日任务进度、必要的幂等处理。
     *
     * @param module  模块标识：word / review / quiz / dict
     * @param minutes 时长（分钟），可为 0
     * @param count   数量（词数 / 题数 / 查词次数）
     */
    void record(Long userId, String module, int minutes, int count);

    /** 确保当日任务已初始化（幂等） */
    void initTodayTasks(Long userId);

    /** 查询当日任务列表 */
    List<TaskRecord> todayTasks(Long userId);

    /** 今日打卡（幂等） */
    void checkinToday(Long userId);

    /** 连续打卡天数 */
    int streakDays(Long userId);

    /** 今日学习时长（分钟） */
    int todayMinutes(Long userId);

    /** 累计学习时长（分钟） */
    int totalMinutes(Long userId);

    /** 今日某模块累计数量 */
    int todayCount(Long userId, String module);

    /** 某天的任务完成情况 [已完成数, 总数] */
    int[] taskProgress(Long userId, LocalDate date);
}
