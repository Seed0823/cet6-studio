package com.cet6.sprint.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * 提交测验答卷
 * <p>
 * 故意不把正确答案下发到前端（{@code QuizQuestionVO} 里没有 correctIndex），
 * 由后端在提交时判分，避免考生直接看接口响应作弊。
 */
@Data
public class QuizSubmitDTO {

    /** 本次自测耗时（秒） */
    private Integer costSecond;

    @NotNull(message = "答题数据不能为空")
    private List<Item> answers;

    @Data
    public static class Item {
        private Long wordId;
        private String question;
        private List<String> options;
        /** 用户选择的选项下标 */
        private Integer chosenIndex;
    }
}
