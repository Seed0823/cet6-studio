package com.cet6.sprint.dto;

import lombok.Data;

/**
 * 完成模考，上报总耗时（秒）
 */
@Data
public class ExamFinishDTO {

    /** 本次模考耗时（秒） */
    private Integer durationSecond;
}
