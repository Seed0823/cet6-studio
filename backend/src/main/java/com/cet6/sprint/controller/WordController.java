package com.cet6.sprint.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cet6.sprint.common.Result;
import com.cet6.sprint.dto.WordGradeDTO;
import com.cet6.sprint.entity.Word;
import com.cet6.sprint.service.WordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 背单词接口
 */
@RestController
@RequestMapping("/api/word")
@RequiredArgsConstructor
public class WordController {

    private final WordService wordService;

    /** 今日新词 */
    @GetMapping("/today")
    public Result<List<Word>> today(@RequestParam(defaultValue = "20") int limit) {
        return Result.ok(wordService.todayWords(limit));
    }

    /** 待复习（艾宾浩斯到期） */
    @GetMapping("/review")
    public Result<List<Word>> review(@RequestParam(defaultValue = "35") int limit) {
        return Result.ok(wordService.reviewWords(limit));
    }

    /** 提交掌握度 */
    @PostMapping("/grade")
    public Result<Void> grade(@Valid @RequestBody WordGradeDTO dto) {
        wordService.submitGrade(dto);
        return Result.ok();
    }

    /** 词汇进度 */
    @GetMapping("/progress")
    public Result<Map<String, Integer>> progress() {
        return Result.ok(wordService.progress());
    }

    /** 词库检索（分页） */
    @GetMapping("/page")
    public Result<Page<Word>> page(@RequestParam(defaultValue = "1") int page,
                                   @RequestParam(defaultValue = "20") int size,
                                   @RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) Integer cet6Only) {
        return Result.ok(wordService.page(page, size, keyword, cet6Only));
    }
}
