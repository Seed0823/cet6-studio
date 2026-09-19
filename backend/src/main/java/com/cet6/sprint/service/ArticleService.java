package com.cet6.sprint.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cet6.sprint.dto.ReadingProgressDTO;
import com.cet6.sprint.vo.ArticleDetailVO;
import com.cet6.sprint.vo.ArticleListVO;
import com.cet6.sprint.vo.ReadingStatsVO;

/**
 * 悦读服务
 */
public interface ArticleService {

    /**
     * 文章列表（分页 + 筛选）
     *
     * @param category       分类：exam / essay / news / science，null 表示全部
     * @param difficulty     难度 1-5，null 表示不限
     * @param keyword        关键词（匹配中英文标题与简介）
     * @param unfinishedOnly 只看未读完
     */
    IPage<ArticleListVO> list(int page, int size, String category, Integer difficulty,
                              String keyword, Boolean unfinishedOnly);

    /**
     * 文章详情
     * <p>
     * 一并返回正文中出现的六级大纲词列表，供前端做「六级词高亮」；
     * 同时带上当前用户对该篇的阅读进度，用于刷新后恢复位置。
     */
    ArticleDetailVO detail(Long articleId);

    /** 记录一次打开（打开次数 +1） */
    void open(Long articleId);

    /** 上报阅读进度与新增时长 */
    void reportProgress(ReadingProgressDTO dto);

    /** 阅读模块概览统计 */
    ReadingStatsVO stats();
}
