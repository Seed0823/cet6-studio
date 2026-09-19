package com.cet6.sprint.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交翻译作答
 */
@Data
public class TranslationSubmitDTO {

    @NotNull(message = "题目不能为空")
    private Long id;

    /** 用户英文作答 */
    private String answer;
}
