package com.cet6.sprint.vo;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 文章详情
 * <p>
 * cet6Words 是正文中出现的六级大纲词（去重、按首次出现顺序）。
 * 它不由数据库存储，而是后端拿内存中的六级词表对正文分词求交集得到 ——
 * 好处是改词表不用重跑内容导入，代价是每篇文章首次访问时要做一次
 * 几百词的比对（微秒级），结果按文章 ID 缓存。
 */
@Data
public class ArticleDetailVO {

    private Long id;
    private String title;
    private String titleCn;
    private String category;
    private String source;
    private String sourceUrl;
    private String author;
    private Integer difficulty;
    private Integer wordCount;
    private String summary;
    private LocalDate publishDate;

    /** 英文正文 */
    private String content;

    /** 中文译文 */
    private String translation;

    /** 正文涉及的六级大纲词 */
    private List<String> cet6Words;

    /** 当前用户的阅读进度 0-100 */
    private Integer progress;

    /** 当前用户是否读完 */
    private Integer finished;

    /** 当前用户累计阅读秒数 */
    private Integer durationSec;
}
