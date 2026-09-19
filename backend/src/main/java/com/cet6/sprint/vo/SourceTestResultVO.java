package com.cet6.sprint.vo;

import lombok.Data;

/**
 * 数据源连通性测试结果
 */
@Data
public class SourceTestResultVO {

    /** 测试类型：dict / pron / translate */
    private String type;

    /** 被测源标识 */
    private String source;

    /** 是否可用 */
    private boolean ok;

    /** 耗时（毫秒） */
    private long costMs;

    /** 说明：成功给样例，失败给原因 */
    private String message;
}
