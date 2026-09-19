package com.cet6.sprint.service;

import com.cet6.sprint.entity.DictQuery;
import com.cet6.sprint.vo.WordLookupVO;
import com.cet6.sprint.vo.WordSuggestVO;

import java.util.List;
import java.util.Map;

/**
 * 查词服务
 * <p>
 * 「查词页」与「做题时点词查询」共用同一套接口，查询次数统一计入热词榜。
 */
public interface DictService {

    /**
     * 查询单词
     * <p>
     * 命中词库则累计查询次数（热词榜数据来源），未命中返回相近词建议。
     */
    WordLookupVO lookup(String word);

    /**
     * 查词自动补全：输入前几个字母返回候选词
     * <p>
     * 只读接口，<b>不</b>累计查询次数 —— 联想出来但用户没点开，不该算进热词榜。
     */
    List<WordSuggestVO> suggest(String prefix, int limit);

    /** 热词榜：按查询次数倒序 */
    List<DictQuery> hot(int limit);

    /** 最近查询 */
    List<DictQuery> recent(int limit);

    /** 查词统计总览 */
    Map<String, Object> stats();

    /** 把某个词加入错词本 */
    void addToWrongBook(Long wordId);
}
