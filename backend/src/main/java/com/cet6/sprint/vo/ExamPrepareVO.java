package com.cet6.sprint.vo;

import lombok.Data;

/**
 * 真题模考组合卷：听力 + 翻译 + 写作 各一份
 */
@Data
public class ExamPrepareVO {

    private ListeningPaperVO listening;
    private TranslationItemVO translation;
    private WritingItemVO writing;
}
