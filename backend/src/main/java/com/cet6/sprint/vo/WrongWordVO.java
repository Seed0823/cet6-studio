package com.cet6.sprint.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 错词本条目（错词记录 + 词库信息）
 */
@Data
public class WrongWordVO {

    private Long wrongId;

    private Long wordId;

    private String word;

    private String phonetic;

    private String pos;

    private String translation;

    /** 来源：quiz / exam / dict */
    private String source;

    private Integer wrongCount;

    /** 0 未攻克 / 1 已攻克 */
    private Integer mastered;

    private LocalDateTime createTime;
}
