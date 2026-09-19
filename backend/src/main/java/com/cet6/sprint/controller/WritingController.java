package com.cet6.sprint.controller;

import com.cet6.sprint.common.Result;
import com.cet6.sprint.dto.WritingSubmitDTO;
import com.cet6.sprint.service.WritingService;
import com.cet6.sprint.vo.WritingItemVO;
import com.cet6.sprint.vo.WritingResultVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 写作练习接口
 */
@RestController
@RequestMapping("/api/writing")
@RequiredArgsConstructor
public class WritingController {

    private final WritingService writingService;

    /** 随机抽取命题作文（不含参考范文） */
    @GetMapping("/random")
    public Result<List<WritingItemVO>> random(@RequestParam(defaultValue = "1") int count) {
        return Result.ok(writingService.random(count));
    }

    /** 提交作文，返回字数校验、关键词覆盖率自测与参考范文 */
    @PostMapping("/submit")
    public Result<WritingResultVO> submit(@Valid @RequestBody WritingSubmitDTO dto) {
        return Result.ok(writingService.submit(dto));
    }
}
