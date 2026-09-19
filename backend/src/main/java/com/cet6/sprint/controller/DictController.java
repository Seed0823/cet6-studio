package com.cet6.sprint.controller;

import com.cet6.sprint.common.Result;
import com.cet6.sprint.entity.DictQuery;
import com.cet6.sprint.service.DictService;
import com.cet6.sprint.vo.WordLookupVO;
import com.cet6.sprint.vo.WordSuggestVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 查词接口
 * <p>
 * 查词页和做题时点词查询共用 /lookup 这一个接口，
 * 每次调用都会累计该词的查询次数，热词榜即由此产生。
 */
@RestController
@RequestMapping("/api/dict")
@RequiredArgsConstructor
public class DictController {

    private final DictService dictService;

    /** 查词（命中则计数 +1） */
    @GetMapping("/lookup")
    public Result<WordLookupVO> lookup(@RequestParam String word) {
        return Result.ok(dictService.lookup(word));
    }

    /**
     * 查词自动补全：输入前几个字母，返回候选词列表
     * <p>
     * 参数名用 q 而不是 word —— 这里传的是「前缀片段」（alle），不是完整单词（alleviate），
     * 语义上区分开，避免前端把半截词误传给 /lookup。
     */
    @GetMapping("/suggest")
    public Result<List<WordSuggestVO>> suggest(@RequestParam("q") String q,
                                              @RequestParam(defaultValue = "10") int limit) {
        return Result.ok(dictService.suggest(q, limit));
    }

    /** 热词榜：按查询次数倒序 */
    @GetMapping("/hot")
    public Result<List<DictQuery>> hot(@RequestParam(defaultValue = "20") int limit) {
        return Result.ok(dictService.hot(limit));
    }

    /** 最近查询 */
    @GetMapping("/recent")
    public Result<List<DictQuery>> recent(@RequestParam(defaultValue = "20") int limit) {
        return Result.ok(dictService.recent(limit));
    }

    /** 查词统计 */
    @GetMapping("/stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(dictService.stats());
    }

    /** 把查到的词加入错词本 */
    @PostMapping("/wrong")
    public Result<Void> addToWrongBook(@RequestParam Long wordId) {
        dictService.addToWrongBook(wordId);
        return Result.ok("已加入错词本", null);
    }
}
