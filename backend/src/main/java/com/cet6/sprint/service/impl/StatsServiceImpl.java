package com.cet6.sprint.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cet6.sprint.common.UserContext;
import com.cet6.sprint.entity.QuizRecord;
import com.cet6.sprint.entity.StudyLog;
import com.cet6.sprint.entity.User;
import com.cet6.sprint.mapper.CheckinMapper;
import com.cet6.sprint.mapper.QuizRecordMapper;
import com.cet6.sprint.mapper.StudyLogMapper;
import com.cet6.sprint.mapper.UserMapper;
import com.cet6.sprint.mapper.WordGradeMapper;
import com.cet6.sprint.mapper.WrongWordMapper;
import com.cet6.sprint.service.StatsService;
import com.cet6.sprint.service.StudyService;
import com.cet6.sprint.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 数据统计服务实现
 */
@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private static final String[] WEEK_CN = {"一", "二", "三", "四", "五", "六", "日"};

    private final UserMapper userMapper;
    private final WordGradeMapper wordGradeMapper;
    private final WrongWordMapper wrongWordMapper;
    private final StudyLogMapper studyLogMapper;
    private final CheckinMapper checkinMapper;
    private final QuizRecordMapper quizRecordMapper;
    private final StudyService studyService;

    @Override
    public DashboardVO dashboard() {
        Long userId = UserContext.getUserId();
        User user = userMapper.selectById(userId);

        // 保证当日任务存在
        studyService.initTodayTasks(userId);

        DashboardVO vo = new DashboardVO();
        vo.setNickname(user == null ? "" : user.getNickname());
        vo.setTargetScore(user == null ? 425 : user.getTargetScore());
        if (user != null && user.getExamDate() != null) {
            vo.setDaysToExam(ChronoUnit.DAYS.between(LocalDate.now(), user.getExamDate()));
        }

        vo.setLearnedWords(wordGradeMapper.countLearned(userId));
        vo.setKnownWords(wordGradeMapper.countKnown(userId));
        vo.setDueReview(wordGradeMapper.countDueReview(userId));
        vo.setUnmasteredWrong(wrongWordMapper.countUnmastered(userId));
        vo.setStreakDays(studyService.streakDays(userId));
        vo.setTotalCheckinDays(checkinMapper.totalDays(userId));
        vo.setTodayMinutes(studyService.todayMinutes(userId));
        vo.setTotalMinutes(studyService.totalMinutes(userId));
        vo.setTasks(studyService.todayTasks(userId));

        int[] progress = studyService.taskProgress(userId, LocalDate.now());
        vo.setTaskFinished(progress[0]);
        vo.setTaskTotal(progress[1]);

        vo.setWeek(buildWeek(userId));
        return vo;
    }

    @Override
    public Map<String, Object> overview(int days) {
        Long userId = UserContext.getUserId();
        int n = Math.max(7, Math.min(days, 90));
        LocalDate from = LocalDate.now().minusDays(n - 1L);

        List<StudyLog> logs = studyLogMapper.range(userId, from);

        // 按日期聚合
        Map<LocalDate, Integer> minutesByDay = new LinkedHashMap<>();
        Map<LocalDate, Integer> wordsByDay = new LinkedHashMap<>();
        for (StudyLog log : logs) {
            minutesByDay.merge(log.getStudyDate(), log.getDurationMin() == null ? 0 : log.getDurationMin(), Integer::sum);
            wordsByDay.merge(log.getStudyDate(), log.getWordCount() == null ? 0 : log.getWordCount(), Integer::sum);
        }

        List<Map<String, Object>> trend = new ArrayList<>();
        LocalDate cursor = from;
        while (!cursor.isAfter(LocalDate.now())) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("date", cursor.toString());
            row.put("minutes", minutesByDay.getOrDefault(cursor, 0));
            row.put("words", wordsByDay.getOrDefault(cursor, 0));
            trend.add(row);
            cursor = cursor.plusDays(1);
        }

        // 测验正确率趋势
        List<QuizRecord> records = quizRecordMapper.selectList(Wrappers.<QuizRecord>lambdaQuery()
                .eq(QuizRecord::getUserId, userId)
                .ge(QuizRecord::getCreateTime, from.atStartOfDay())
                .orderByAsc(QuizRecord::getCreateTime));
        BigDecimal avgAccuracy = records.isEmpty() ? BigDecimal.ZERO
                : BigDecimal.valueOf(records.stream()
                        .map(r -> r.getAccuracy() == null ? BigDecimal.ZERO : r.getAccuracy())
                        .mapToDouble(BigDecimal::doubleValue)
                        .average().orElse(0))
                .setScale(1, RoundingMode.HALF_UP);

        Map<String, Object> quizStat = new LinkedHashMap<>();
        quizStat.put("count", records.size());
        quizStat.put("avgAccuracy", avgAccuracy);
        quizStat.put("trend", records.stream().map(r -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("time", r.getCreateTime() == null ? "" : r.getCreateTime().format(DateTimeFormatter.ofPattern("MM-dd HH:mm")));
            m.put("accuracy", r.getAccuracy());
            m.put("correct", r.getCorrect());
            m.put("total", r.getTotal());
            return m;
        }).collect(Collectors.toList()));

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("totalMinutes", studyService.totalMinutes(userId));
        summary.put("todayMinutes", studyService.todayMinutes(userId));
        summary.put("learnedWords", wordGradeMapper.countLearned(userId));
        summary.put("knownWords", wordGradeMapper.countKnown(userId));
        summary.put("dueReview", wordGradeMapper.countDueReview(userId));
        summary.put("unmasteredWrong", wrongWordMapper.countUnmastered(userId));
        summary.put("streakDays", studyService.streakDays(userId));
        summary.put("totalCheckinDays", checkinMapper.totalDays(userId));
        summary.put("quizCount", records.size());
        summary.put("avgAccuracy", avgAccuracy);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("summary", summary);
        result.put("trend", trend);
        result.put("quiz", quizStat);
        return result;
    }

    /** 最近 7 天学习时长 */
    private List<DashboardVO.DayStat> buildWeek(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate from = today.minusDays(6);
        List<StudyLog> logs = studyLogMapper.range(userId, from);

        Map<LocalDate, Integer> minutesByDay = logs.stream().collect(Collectors.toMap(
                StudyLog::getStudyDate,
                l -> l.getDurationMin() == null ? 0 : l.getDurationMin(),
                Integer::sum));

        List<DashboardVO.DayStat> week = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            LocalDate d = from.plusDays(i);
            DashboardVO.DayStat stat = new DashboardVO.DayStat();
            stat.setDate(d.toString());
            stat.setLabel("周" + WEEK_CN[d.getDayOfWeek().getValue() - 1]);
            stat.setMinutes(minutesByDay.getOrDefault(d, 0));
            stat.setToday(d.equals(today));
            week.add(stat);
        }
        return week;
    }
}
