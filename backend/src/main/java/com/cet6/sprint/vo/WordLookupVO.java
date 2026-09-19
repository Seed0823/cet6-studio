package com.cet6.sprint.vo;

import lombok.Data;

import java.util.List;

/**
 * 查词结果（查词页 / 做题点词 共用同一份结构）
 */
@Data
public class WordLookupVO {

    private Long wordId;

    private String word;

    private String phonetic;

    private String pos;

    /** 英文释义 */
    private String definition;

    /** 中文释义 */
    private String translation;

    private String tag;

    private String exchange;

    /** 是否六级大纲词 */
    private boolean cet6;

    /** 该词被当前用户累计查询次数（查询后即时值） */
    private Integer queryCount;

    /** 是否精确命中词库 */
    private boolean exactMatch;

    /** 未精确命中时，给出的相近词建议 */
    private List<String> suggestions;

    /** 是否已在错词本中 */
    private boolean inWrongBook;

    /**
     * 外部源增强信息（音标 / 词性 / 英文释义 / 发音直链）
     * <p>
     * 可能为 null —— 外部源关闭、不可用或该词未收录时都不返回，
     * 此时前端照常展示本地词库的内容，并回退到浏览器 TTS 发音。
     */
    private ExternalDictVO external;
}
