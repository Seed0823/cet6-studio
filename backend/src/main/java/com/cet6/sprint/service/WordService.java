package com.cet6.sprint.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cet6.sprint.dto.WordGradeDTO;
import com.cet6.sprint.entity.Word;

import java.util.List;
import java.util.Map;

/**
 * 背单词服务
 */
public interface WordService {

    /** 今日新词（优先未学过的六级词） */
    List<Word> todayWords(int limit);

    /** 今日到期需复习的词（艾宾浩斯） */
    List<Word> reviewWords(int limit);

    /** 提交掌握度 */
    void submitGrade(WordGradeDTO dto);

    /** 词库分页检索 */
    Page<Word> page(int pageNo, int size, String keyword, Integer cet6Only);

    /** 我的词汇进度 */
    Map<String, Integer> progress();
}
