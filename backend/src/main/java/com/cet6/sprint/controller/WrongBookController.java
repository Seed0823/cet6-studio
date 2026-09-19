package com.cet6.sprint.controller;

import com.cet6.sprint.common.Result;
import com.cet6.sprint.service.WrongBookService;
import com.cet6.sprint.vo.WrongWordVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 错词本接口
 */
@RestController
@RequestMapping("/api/wrong")
@RequiredArgsConstructor
public class WrongBookController {

    private final WrongBookService wrongBookService;

    /** 错词列表，mastered 不传则查全部 */
    @GetMapping("/list")
    public Result<List<WrongWordVO>> list(@RequestParam(required = false) Integer mastered) {
        return Result.ok(wrongBookService.list(mastered));
    }

    /** 标记攻克 / 取消攻克 */
    @PostMapping("/master")
    public Result<Void> master(@RequestParam Long id,
                               @RequestParam(defaultValue = "true") boolean mastered) {
        wrongBookService.markMastered(id, mastered);
        return Result.ok();
    }

    /** 移出错词本 */
    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        wrongBookService.remove(id);
        return Result.ok("已移除", null);
    }
}
