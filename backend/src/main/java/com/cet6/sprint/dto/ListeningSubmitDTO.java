package com.cet6.sprint.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 提交听力答卷
 */
@Data
public class ListeningSubmitDTO {

    @NotNull(message = "听力题目不能为空")
    private Long id;

    private List<Item> answers;

    @Data
    public static class Item {
        private Long questionId;
        private Integer chosenIndex;
    }
}
