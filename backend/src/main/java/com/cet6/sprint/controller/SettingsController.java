package com.cet6.sprint.controller;

import com.cet6.sprint.common.Result;
import com.cet6.sprint.service.ExternalSourceService;
import com.cet6.sprint.service.SettingsService;
import com.cet6.sprint.vo.SettingsVO;
import com.cet6.sprint.vo.SourceTestResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 设置接口（外部数据源配置）
 */
@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
public class SettingsController {

    private final SettingsService settingsService;
    private final ExternalSourceService externalSourceService;

    /** 读取配置（含各源可选项，前端下拉直接用它渲染） */
    @GetMapping
    public Result<SettingsVO> get() {
        return Result.ok(settingsService.get());
    }

    /** 保存配置 */
    @PutMapping
    public Result<Void> update(@RequestBody SettingsVO vo) {
        settingsService.update(vo);
        return Result.ok("已保存", null);
    }

    /**
     * 连通性测试
     * <p>
     * 设置页「测试」按钮调这个：真实发起一次外部请求，返回可用性与耗时。
     */
    @PostMapping("/test")
    public Result<SourceTestResultVO> test(@RequestParam String type) {
        return Result.ok(externalSourceService.test(type));
    }
}
