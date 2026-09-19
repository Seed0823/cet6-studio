package com.cet6.sprint.vo;

import lombok.Data;

/**
 * 查词自动补全候选
 * <p>
 * 刻意做得比 {@link WordLookupVO} 轻量：一屏要展示 10 条，
 * 只带「单词 + 首条释义摘要 + 是否六级词」够用，不传完整释义和词形变化，
 * 减少响应体体积和前端渲染压力。
 */
@Data
public class WordSuggestVO {

    /** 词库主键（选中后可直接加入错词本，免去二次查询） */
    private Long wordId;

    /** 单词（el-autocomplete 的 value-key） */
    private String word;

    /** 音标 */
    private String phonetic;

    /** 中文释义摘要：取首条义项，超长截断 */
    private String brief;

    /** 是否六级大纲词，前端显示「六级」标签 */
    private boolean cet6;
}
