package com.cet6.sprint.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交写作作答
 */
@Data
public class WritingSubmitDTO {

    @NotNull(message = "题目不能为空")
    private Long id;

    /** 用户英文作文 */
    private String answer;
}
