package com.cet6.sprint.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 阅读文章
 * <p>
 * content 与 translation 都按「段」存储，段落之间以空行分隔，且两列段落
 * 严格一一对应 —— 前端据此做逐段对照，而不是整篇并排。这一点是刻意的：
 * 逐段对齐时读者可以只看英文、需要时再看对应译文，整篇并排则会被迫在
 * 两栏之间来回跳。内容导入脚本会校验段落数一致，不一致直接拒绝入库。
 */
@Data
@TableName("t_article")
public class Article {

    @TableId(type = IdType.AUTO)
    private Long id;

    /** 英文标题 */
    private String title;

    /** 中文标题 */
    private String titleCn;

    /** 分类：exam 真题仿真 / essay 公版美文 / news 外刊新闻 / science 科普短文 */
    private String category;

    /** 来源说明 */
    private String source;

    /** 原文链接 */
    private String sourceUrl;

    /** 作者 */
    private String author;

    /** 难度 1-5 */
    private Integer difficulty;

    /** 英文词数 */
    private Integer wordCount;

    /** 中文简介 */
    private String summary;

    /** 英文正文（段落以空行分隔） */
    private String content;

    /** 中文译文（段落与 content 一一对应） */
    private String translation;

    /** 原文发布日期 */
    private LocalDate publishDate;

    private LocalDateTime createTime;
}
