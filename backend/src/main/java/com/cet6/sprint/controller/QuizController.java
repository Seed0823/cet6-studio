package com.cet6.sprint.controller;

import com.cet6.sprint.common.Result;
import com.cet6.sprint.dto.QuizSubmitDTO;
import com.cet6.sprint.entity.QuizRecord;
import com.cet6.sprint.service.QuizService;
import com.cet6.sprint.vo.QuizQuestionVO;
import com.cet6.sprint.vo.QuizResultVO;
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
 * 单词自测接口
 */
@RestController
@RequestMapping("/api/quiz")
@RequiredArgsConstructor
public class QuizController {

    private final QuizService quizService;

    /** 生成题目（不含答案） */
    @GetMapping("/generate")
    public Result<List<QuizQuestionVO>> generate(@RequestParam(defaultValue = "10") int count) {
        return Result.ok(quizService.generate(count));
    }

    /** 提交答卷 */
    @PostMapping("/submit")
    public Result<QuizResultVO> submit(@Valid @RequestBody QuizSubmitDTO dto) {
        return Result.ok("已提交", quizService.submit(dto));
    }

    /** 历史记录 */
    @GetMapping("/history")
    public Result<List<QuizRecord>> history(@RequestParam(defaultValue = "10") int limit) {
        return Result.ok(quizService.history(limit));
    }
}
