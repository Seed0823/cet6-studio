package com.cet6.sprint.controller;

import com.cet6.sprint.common.Result;
import com.cet6.sprint.dto.ListeningSubmitDTO;
import com.cet6.sprint.service.ListeningService;
import com.cet6.sprint.vo.ListeningPaperVO;
import com.cet6.sprint.vo.ListeningResultVO;
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
 * 听力练习接口
 */
@RestController
@RequestMapping("/api/listening")
@RequiredArgsConstructor
public class ListeningController {

    private final ListeningService listeningService;

    /** 随机抽取听力试卷（不含答案） */
    @GetMapping("/random")
    public Result<List<ListeningPaperVO>> random(@RequestParam(defaultValue = "1") int count) {
        return Result.ok(listeningService.randomPaper(count));
    }

    /** 提交答卷 */
    @PostMapping("/submit")
    public Result<ListeningResultVO> submit(@Valid @RequestBody ListeningSubmitDTO dto) {
        return Result.ok(listeningService.submit(dto));
    }
}
