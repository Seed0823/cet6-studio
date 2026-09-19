package com.cet6.sprint.service.impl;

import com.cet6.sprint.entity.TaskRecord;
import com.cet6.sprint.mapper.CheckinMapper;
import com.cet6.sprint.mapper.StudyLogMapper;
import com.cet6.sprint.mapper.TaskRecordMapper;
import com.cet6.sprint.service.StudyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

/**
 * 学习记录服务实现
 */
@Service
@RequiredArgsConstructor
public class StudyServiceImpl implements StudyService {

    private final StudyLogMapper studyLogMapper;
    private final TaskRecordMapper taskRecordMapper;
    private final CheckinMapper checkinMapper;

    @Value("${cet6.daily-task.word-target:20}")
    private int wordTarget;
    @Value("${cet6.daily-task.review-target:35}")
    private int reviewTarget;
    @Value("${cet6.daily-task.quiz-target:10}")
    private int quizTarget;
    @Value("${cet6.daily-task.dict-target:5}")
    private int dictTarget;

    @Override
    public void record(Long userId, String module, int minutes, int count) {
        if (minutes <= 0 && count <= 0) {
            return;
        }
        LocalDate today = LocalDate.now();
        // 1. 学习日志按 (用户, 日期, 模块) 聚合累加
        studyLogMapper.accumulate(userId, today, module, Math.max(minutes, 0), Math.max(count, 0));
        // 2. 用当日累计值刷新任务进度（取当日总数而不是增量，天然幂等）
        int todayCount = studyLogMapper.countOfDay(userId, today, module);
        taskRecordMapper.upsertProgress(userId, today, module, taskNameOf(module),
                targetOf(module), todayCount);
    }

    @Override
    public void initTodayTasks(Long userId) {
        LocalDate today = LocalDate.now();
        taskRecordMapper.initTask(userId, today, TaskRecord.KEY_WORD, taskNameOf(TaskRecord.KEY_WORD), wordTarget);
        taskRecordMapper.initTask(userId, today, TaskRecord.KEY_REVIEW, taskNameOf(TaskRecord.KEY_REVIEW), reviewTarget);
        taskRecordMapper.initTask(userId, today, TaskRecord.KEY_QUIZ, taskNameOf(TaskRecord.KEY_QUIZ), quizTarget);
        taskRecordMapper.initTask(userId, today, TaskRecord.KEY_DICT, taskNameOf(TaskRecord.KEY_DICT), dictTarget);
    }

    @Override
    public List<TaskRecord> todayTasks(Long userId) {
        return taskRecordMapper.listOfDay(userId, LocalDate.now());
    }

    @Override
    public void checkinToday(Long userId) {
        checkinMapper.checkin(userId, LocalDate.now());
    }

    @Override
    public int streakDays(Long userId) {
        List<LocalDate> dates = checkinMapper.recentDates(userId);
        if (dates.isEmpty()) {
            return 0;
        }
        // 以「今天」为基准；今天还没打卡则从昨天开始算（当天仍算连续）
        LocalDate cursor = LocalDate.now();
        if (!dates.get(0).equals(cursor)) {
            cursor = cursor.minusDays(1);
        }
        int streak = 0;
        for (LocalDate d : dates) {
            if (d.equals(cursor)) {
                streak++;
                cursor = cursor.minusDays(1);
            } else if (d.isBefore(cursor)) {
                break;
            }
        }
        return streak;
    }

    @Override
    public int todayMinutes(Long userId) {
        return studyLogMapper.minutesOfDay(userId, LocalDate.now());
    }

    @Override
    public int totalMinutes(Long userId) {
        return studyLogMapper.totalMinutes(userId);
    }

    @Override
    public int todayCount(Long userId, String module) {
        return studyLogMapper.countOfDay(userId, LocalDate.now(), module);
    }

    @Override
    public int[] taskProgress(Long userId, LocalDate date) {
        List<TaskRecord> list = taskRecordMapper.listOfDay(userId, date);
        int total = list.size();
        int finished = (int) list.stream()
                .filter(t -> t.getStatus() != null && t.getStatus() == 1)
                .count();
        return new int[]{finished, total};
    }

    private String taskNameOf(String module) {
        return switch (module) {
            case TaskRecord.KEY_WORD -> "新词学习";
            case TaskRecord.KEY_REVIEW -> "艾宾浩斯复习";
            case TaskRecord.KEY_QUIZ -> "单词自测";
            case TaskRecord.KEY_DICT -> "查词积累";
            default -> module;
        };
    }

    private int targetOf(String module) {
        return switch (module) {
            case TaskRecord.KEY_WORD -> wordTarget;
            case TaskRecord.KEY_REVIEW -> reviewTarget;
            case TaskRecord.KEY_QUIZ -> quizTarget;
            case TaskRecord.KEY_DICT -> dictTarget;
            default -> 0;
        };
    }
}
