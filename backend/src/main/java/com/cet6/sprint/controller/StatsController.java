package com.cet6.sprint.controller;

import com.cet6.sprint.common.Result;
import com.cet6.sprint.service.StatsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 学习统计接口
 */
@RestController
@RequestMapping("/api/stats")
@RequiredArgsConstructor
public class StatsController {

    private final StatsService statsService;

    /** 统计总览 + 趋势 */
    @GetMapping("/overview")
    public Result<Map<String, Object>> overview(@RequestParam(defaultValue = "30") int days) {
        return Result.ok(statsService.overview(days));
    }
}
