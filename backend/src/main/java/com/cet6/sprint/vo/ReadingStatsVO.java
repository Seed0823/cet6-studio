package com.cet6.sprint.vo;

import lombok.Data;

import java.util.List;

/**
 * 阅读模块统计（阅读列表页顶部的概览条）
 */
@Data
public class ReadingStatsVO {

    /** 文章总数 */
    private Integer total;

    /** 已读完篇数 */
    private Integer finishedCount;

    /** 在读篇数（有进度但未读完） */
    private Integer readingCount;

    /** 累计阅读时长（分钟） */
    private Integer totalMinutes;

    /** 已精读到的六级词数（去重） */
    private Integer cet6WordCount;

    /** 分类维度 */
    private List<CategoryStat> categories;

    @Data
    public static class CategoryStat {
        private String category;
        /** 该类文章总数 */
        private Integer total;
        /** 该类已读完数 */
        private Integer finished;
    }
}
