package com.cet6.sprint.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 设置页数据（外部数据源配置）
 * <p>
 * 同时返回「当前值」和「可选项」，让前端不必硬编码源列表 ——
 * 后端新增一个源，前端下拉框自动多一项。
 */
@Data
public class SettingsVO {

    /** 外部数据源总开关 */
    private boolean enabled;

    /** 词典源：datamuse / dictionaryapi / off */
    private String dictSource;
    /** 词典源自定义地址，空串=用内置默认 */
    private String dictBaseUrl;

    /** 发音源：youdao / dictionaryapi / off */
    private String pronSource;
    private String pronBaseUrl;

    /** 翻译源：mymemory / off */
    private String translateSource;
    private String translateBaseUrl;

    /** 外部结果缓存分钟数 */
    private Integer cacheMinutes;

    /** 单次外部请求超时（毫秒） */
    private Integer timeoutMs;

    /** 可选源列表（供前端渲染下拉） */
    private List<Option> dictOptions;
    private List<Option> pronOptions;
    private List<Option> translateOptions;

    /** 下拉选项 */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Option {
        private String value;
        private String label;
        private String desc;
    }
}
