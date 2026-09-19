package com.cet6.sprint.controller;

import com.cet6.sprint.common.Result;
import com.cet6.sprint.service.StatsService;
import com.cet6.sprint.vo.DashboardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 今日学习（首页看板）
 */
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final StatsService statsService;

    @GetMapping
    public Result<DashboardVO> dashboard() {
        return Result.ok(statsService.dashboard());
    }
}
