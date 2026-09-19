package com.cet6.sprint.controller;

import com.cet6.sprint.common.Result;
import com.cet6.sprint.dto.TranslationSubmitDTO;
import com.cet6.sprint.service.TranslationService;
import com.cet6.sprint.vo.TranslationItemVO;
import com.cet6.sprint.vo.TranslationResultVO;
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
 * 翻译练习接口
 */
@RestController
@RequestMapping("/api/translation")
@RequiredArgsConstructor
public class TranslationController {

    private final TranslationService translationService;

    /** 随机抽取练习题目（不含参考译文） */
    @GetMapping("/random")
    public Result<List<TranslationItemVO>> random(@RequestParam(defaultValue = "1") int count) {
        return Result.ok(translationService.random(count));
    }

    /** 提交译文，返回参考与覆盖率自测 */
    @PostMapping("/submit")
    public Result<TranslationResultVO> submit(@Valid @RequestBody TranslationSubmitDTO dto) {
        return Result.ok(translationService.submit(dto));
    }
}
