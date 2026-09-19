package com.cet6.sprint.vo;

import lombok.Data;

import java.util.List;

/**
 * 听力提交结果
 */
@Data
public class ListeningResultVO {

    private Long id;
    private Integer total;
    private Integer correct;

    private List<Item> details;

    @Data
    public static class Item {
        private Long questionId;
        private String question;
        private List<String> options;
        private Integer chosenIndex;
        private Integer correctIndex;
        private String explain;
        private boolean right;
    }
}
