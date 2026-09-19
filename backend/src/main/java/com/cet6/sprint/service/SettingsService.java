package com.cet6.sprint.service;

import com.cet6.sprint.vo.SettingsVO;

/**
 * 应用配置读写（设置页）
 */
public interface SettingsService {

    /** 读取当前配置（含可选项列表，供前端渲染下拉） */
    SettingsVO get();

    /** 保存配置（带白名单校验） */
    void update(SettingsVO vo);

    /** 读取单个配置值 */
    String value(String key, String def);

    /** 读取单个配置的整数形式 */
    int intValue(String key, int def);

    /** 配置键常量 */
    final class Keys {
        public static final String ENABLED = "ext.enabled";
        public static final String DICT_SOURCE = "ext.dict.source";
        public static final String DICT_BASE_URL = "ext.dict.baseUrl";
        public static final String PRON_SOURCE = "ext.pron.source";
        public static final String PRON_BASE_URL = "ext.pron.baseUrl";
        public static final String TRANSLATE_SOURCE = "ext.translate.source";
        public static final String TRANSLATE_BASE_URL = "ext.translate.baseUrl";
        public static final String CACHE_MINUTES = "ext.cacheMinutes";
        public static final String TIMEOUT_MS = "ext.timeoutMs";

        private Keys() {
        }
    }
}
