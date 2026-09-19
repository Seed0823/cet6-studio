package com.cet6.sprint.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 提交单词掌握度
 */
@Data
public class WordGradeDTO {

    @NotNull(message = "单词 ID 不能为空")
    private Long wordId;

    /** 0 不认识 / 1 模糊 / 2 认识 */
    @NotNull(message = "掌握程度不能为空")
    @Min(value = 0, message = "掌握程度取值 0-2")
    @Max(value = 2, message = "掌握程度取值 0-2")
    private Integer grade;

    /** 本次学习耗时（分钟），用于累计学习时长 */
    private Integer durationMin;

    /** 是否来自「艾宾浩斯复习」入口（用于区分每日任务归属） */
    private Boolean fromReview;
}
