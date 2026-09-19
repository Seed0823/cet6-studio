package com.cet6.sprint.vo;

import com.cet6.sprint.entity.TaskRecord;
import lombok.Data;

import java.util.List;

/**
 * 今日学习页数据
 */
@Data
public class DashboardVO {

    private String nickname;

    /** 距考试天数 */
    private Long daysToExam;

    private Integer targetScore;

    /** 已学单词数 */
    private Integer learnedWords;

    /** 已掌握（grade=2） */
    private Integer knownWords;

    /** 待复习（艾宾浩斯到期） */
    private Integer dueReview;

    /** 未攻克错词数 */
    private Integer unmasteredWrong;

    /** 连续打卡天数 */
    private Integer streakDays;

    /** 累计打卡天数 */
    private Integer totalCheckinDays;

    /** 今日学习时长（分钟） */
    private Integer todayMinutes;

    /** 累计学习时长（分钟） */
    private Integer totalMinutes;

    /** 今日任务完成数 / 总数 */
    private Integer taskFinished;

    private Integer taskTotal;

    private List<TaskRecord> tasks;

    /** 最近 7 天学习时长 */
    private List<DayStat> week;

    @Data
    public static class DayStat {
        private String date;
        private String label;
        private Integer minutes;
        /** 是否今天 */
        private Boolean today;
    }
}
