package com.cet6.sprint.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cet6.sprint.common.Result;
import com.cet6.sprint.dto.ReadingProgressDTO;
import com.cet6.sprint.service.ArticleService;
import com.cet6.sprint.vo.ArticleDetailVO;
import com.cet6.sprint.vo.ArticleListVO;
import com.cet6.sprint.vo.ReadingStatsVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 悦读接口
 * <p>
 * 路径设计上把 /list 与 /stats 放在 /{id} 之前，靠字面量优先于路径变量
 * 来避免「stats 被当成文章 ID」的歧义。
 */
@RestController
@RequestMapping("/api/reading")
@RequiredArgsConstructor
public class ArticleController {

    private final ArticleService articleService;

    /** 文章列表（分页 + 分类/难度/关键词筛选） */
    @GetMapping("/list")
    public Result<IPage<ArticleListVO>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "9") int size,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Integer difficulty,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean unfinishedOnly) {
        return Result.ok(articleService.list(page, size, category, difficulty, keyword, unfinishedOnly));
    }

    /** 阅读模块概览统计 */
    @GetMapping("/stats")
    public Result<ReadingStatsVO> stats() {
        return Result.ok(articleService.stats());
    }

    /** 文章详情（含译文、六级词、当前用户进度） */
    @GetMapping("/{id}")
    public Result<ArticleDetailVO> detail(@PathVariable Long id) {
        return Result.ok(articleService.detail(id));
    }

    /** 记录一次打开 */
    @PostMapping("/{id}/open")
    public Result<Void> open(@PathVariable Long id) {
        articleService.open(id);
        return Result.ok();
    }

    /** 上报阅读进度与新增时长 */
    @PostMapping("/progress")
    public Result<Void> progress(@Valid @RequestBody ReadingProgressDTO dto) {
        articleService.reportProgress(dto);
        return Result.ok();
    }
}
