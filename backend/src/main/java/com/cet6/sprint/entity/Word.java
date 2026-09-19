package com.cet6.sprint.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 词库（ECDICT 导入）
 * <p>
 * 同时服务两个场景：
 * 1. 背单词 —— 取 tag 含 cet6 的 5400 词
 * 2. 查词 —— 全量 5.7 万词条，覆盖阅读/听力做题时点到的词
 */
@Data
@TableName("t_word")
public class Word {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String word;

    /** 音标 */
    private String phonetic;

    /** 词性 */
    private String pos;

    /** 英文释义 */
    private String definition;

    /** 中文释义（换行符分隔多个义项） */
    private String translation;

    /** 考试标签：cet4/cet6/ky/toefl/ielts/gre */
    private String tag;

    /** 柯林斯星级 0-5 */
    private Integer collins;

    /** 是否牛津三千核心词 */
    private Integer oxford;

    /** BNC 词频序（越小越高频） */
    private Integer bnc;

    /** 当代语料库词频序 */
    private Integer frq;

    /** 词形变化 */
    private String exchange;

    /** 是否六级大纲词（0/1）。冗余字段，导入时回填，便于走索引避免 LIKE 全表扫描 */
    private Integer isCet6;
}
