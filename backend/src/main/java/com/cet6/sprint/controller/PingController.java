package com.cet6.sprint.controller;

import com.cet6.sprint.common.Result;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查（无需登录）
 */
@RestController
public class PingController {

    @GetMapping("/api/ping")
    public Result<Map<String, Object>> ping() {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("status", "UP");
        map.put("time", LocalDateTime.now().toString());
        return Result.ok(map);
    }
}
