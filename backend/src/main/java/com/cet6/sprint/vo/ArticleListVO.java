package com.cet6.sprint.vo;

import lombok.Data;

import java.time.LocalDate;

/**
 * 文章列表项（不含正文，避免列表接口把几百 KB 正文一起传下去）
 * <p>
 * progress / finished / durationSec 来自 t_reading_record 的 LEFT JOIN，
 * 未读过的文章为 0 —— 列表页据此渲染「未读 / 读到 62% / 已读完」三种状态。
 */
@Data
public class ArticleListVO {

    private Long id;
    private String title;
    private String titleCn;
    private String category;
    private String source;
    private String author;
    private Integer difficulty;
    private Integer wordCount;
    private String summary;
    private LocalDate publishDate;

    /** 阅读进度 0-100 */
    private Integer progress;

    /** 是否读完 0/1 */
    private Integer finished;

    /** 累计阅读时长（秒） */
    private Integer durationSec;
}
