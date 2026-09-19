package com.cet6.sprint.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

/**
 * 测验结果
 */
@Data
public class QuizResultVO {

    private Long recordId;

    private Integer total;

    private Integer correct;

    /** 正确率，百分比 */
    private BigDecimal accuracy;

    private Integer costSecond;

    private List<Item> details;

    @Data
    public static class Item {
        private Long wordId;
        private String word;
        private String phonetic;
        private String question;
        private List<String> options;
        private Integer chosenIndex;
        private Integer correctIndex;
        private String correctTranslation;
        private boolean right;
    }
}
