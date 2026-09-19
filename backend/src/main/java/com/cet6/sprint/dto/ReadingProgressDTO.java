package com.cet6.sprint.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 阅读进度上报
 * <p>
 * 前端在滚动停止、切后台、离开页面时上报。addSeconds 传的是「自上次上报
 * 以来新增的秒数」而不是累计秒数 —— 服务端做累加，避免前端重算出错或
 * 重复提交把时长翻倍。progress 服务端取最大值，所以重复提交是安全的。
 */
@Data
public class ReadingProgressDTO {

    @NotNull(message = "文章 ID 不能为空")
    private Long articleId;

    /** 阅读进度 0-100 */
    @Min(value = 0, message = "进度不能小于 0")
    @Max(value = 100, message = "进度不能大于 100")
    private Integer progress = 0;

    /** 本次新增阅读秒数（前端两次上报之间的间隔） */
    @Min(value = 0, message = "时长不能为负")
    private Integer addSeconds = 0;
}
