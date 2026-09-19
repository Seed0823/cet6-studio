package com.cet6.sprint.service.impl;

import com.cet6.sprint.common.BusinessException;
import com.cet6.sprint.entity.AppConfig;
import com.cet6.sprint.external.ExternalApiClient;
import com.cet6.sprint.mapper.AppConfigMapper;
import com.cet6.sprint.service.SettingsService;
import com.cet6.sprint.vo.SettingsVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 应用配置实现
 * <p>
 * 三级取值：DB 覆盖值 → 内置默认值。配置项很少，整表读出后在内存缓存 30 秒，
 * 避免每次查词都打一次库（查词是高频操作）。
 * 保存时主动失效缓存，所以设置页改完立刻生效，不用等 30 秒。
 */
@Service
@RequiredArgsConstructor
public class SettingsServiceImpl implements SettingsService {

    /** 内存缓存时长：30 秒。够短，运维直接改库也能较快生效 */
    private static final long CACHE_MS = 30_000L;

    /** 内置默认值 */
    private static final Map<String, String> DEFAULTS = new LinkedHashMap<>();

    static {
        DEFAULTS.put(Keys.ENABLED, "1");
        DEFAULTS.put(Keys.DICT_SOURCE, "datamuse");
        DEFAULTS.put(Keys.DICT_BASE_URL, "");
        DEFAULTS.put(Keys.PRON_SOURCE, "youdao");
        DEFAULTS.put(Keys.PRON_BASE_URL, "");
        DEFAULTS.put(Keys.TRANSLATE_SOURCE, "mymemory");
        DEFAULTS.put(Keys.TRANSLATE_BASE_URL, "");
        DEFAULTS.put(Keys.CACHE_MINUTES, "10");
        DEFAULTS.put(Keys.TIMEOUT_MS, "6000");
    }

    /** 各源的白名单：value → 展示名|说明 */
    private static final List<SettingsVO.Option> DICT_OPTIONS = Arrays.asList(
            new SettingsVO.Option("datamuse", "Datamuse（推荐）", "免密钥，实测稳定 0.8s；给词性 + 音标 + 英文释义"),
            new SettingsVO.Option("dictionaryapi", "Free Dictionary API", "免密钥；2026-09 实测源站故障（未缓存词返回 522），保留备选"),
            new SettingsVO.Option("off", "关闭", "只用本地词库，不发起任何外部请求")
    );

    private static final List<SettingsVO.Option> PRON_OPTIONS = Arrays.asList(
            new SettingsVO.Option("youdao", "有道发音（推荐）", "免密钥，实测 0.38s，直接返回 mp3"),
            new SettingsVO.Option("dictionaryapi", "Free Dictionary API 音频", "免密钥；音频托管在维基共享，可用性依赖上游"),
            new SettingsVO.Option("off", "关闭", "回退到浏览器内置语音合成（TTS）")
    );

    private static final List<SettingsVO.Option> TRANSLATE_OPTIONS = Arrays.asList(
            new SettingsVO.Option("mymemory", "MyMemory（推荐）", "免密钥，实测 1.3s；单次上限 500 字符，后端自动分段"),
            new SettingsVO.Option("off", "关闭", "只给关键点覆盖率自测，不给机器参考译文")
    );

    private final AppConfigMapper appConfigMapper;
    private final ExternalApiClient externalApiClient;

    private volatile Map<String, String> cache;
    private volatile long cachedAt;

    // ------------------------------------------------------------
    // 读取
    // ------------------------------------------------------------

    @Override
    public SettingsVO get() {
        Map<String, String> c = current();
        SettingsVO vo = new SettingsVO();
        vo.setEnabled(!"0".equals(c.get(Keys.ENABLED)));
        vo.setDictSource(c.get(Keys.DICT_SOURCE));
        vo.setDictBaseUrl(c.get(Keys.DICT_BASE_URL));
        vo.setPronSource(c.get(Keys.PRON_SOURCE));
        vo.setPronBaseUrl(c.get(Keys.PRON_BASE_URL));
        vo.setTranslateSource(c.get(Keys.TRANSLATE_SOURCE));
        vo.setTranslateBaseUrl(c.get(Keys.TRANSLATE_BASE_URL));
        vo.setCacheMinutes(parseInt(c.get(Keys.CACHE_MINUTES), 10));
        vo.setTimeoutMs(parseInt(c.get(Keys.TIMEOUT_MS), 6000));
        vo.setDictOptions(DICT_OPTIONS);
        vo.setPronOptions(PRON_OPTIONS);
        vo.setTranslateOptions(TRANSLATE_OPTIONS);
        return vo;
    }

    @Override
    public String value(String key, String def) {
        String v = current().get(key);
        return v == null || v.isBlank() ? def : v;
    }

    @Override
    public int intValue(String key, int def) {
        return parseInt(current().get(key), def);
    }

    // ------------------------------------------------------------
    // 写入
    // ------------------------------------------------------------

    @Override
    public void update(SettingsVO vo) {
        Map<String, String> toSave = new LinkedHashMap<>();
        toSave.put(Keys.ENABLED, vo.isEnabled() ? "1" : "0");
        toSave.put(Keys.DICT_SOURCE, checkSource("词典", vo.getDictSource(), DICT_OPTIONS));
        toSave.put(Keys.PRON_SOURCE, checkSource("发音", vo.getPronSource(), PRON_OPTIONS));
        toSave.put(Keys.TRANSLATE_SOURCE, checkSource("翻译", vo.getTranslateSource(), TRANSLATE_OPTIONS));
        toSave.put(Keys.DICT_BASE_URL, checkUrl("词典", vo.getDictBaseUrl()));
        toSave.put(Keys.PRON_BASE_URL, checkUrl("发音", vo.getPronBaseUrl()));
        toSave.put(Keys.TRANSLATE_BASE_URL, checkUrl("翻译", vo.getTranslateBaseUrl()));
        toSave.put(Keys.CACHE_MINUTES, String.valueOf(clamp(
                vo.getCacheMinutes() == null ? 10 : vo.getCacheMinutes(), 0, 1440, "缓存时长")));
        toSave.put(Keys.TIMEOUT_MS, String.valueOf(clamp(
                vo.getTimeoutMs() == null ? 6000 : vo.getTimeoutMs(), 1000, 30000, "请求超时")));

        toSave.forEach(appConfigMapper::upsert);

        // 立即失效缓存：设置页保存后马上生效
        synchronized (this) {
            cache = null;
            cachedAt = 0;
        }
        // 换源/改地址后，旧缓存不能再返回
        externalApiClient.clearCache();
    }

    // ------------------------------------------------------------
    // 内部
    // ------------------------------------------------------------

    /** 带缓存的整表读取 */
    private Map<String, String> current() {
        Map<String, String> c = cache;
        if (c != null && System.currentTimeMillis() - cachedAt < CACHE_MS) {
            return c;
        }
        synchronized (this) {
            if (cache != null && System.currentTimeMillis() - cachedAt < CACHE_MS) {
                return cache;
            }
            Map<String, String> loaded = new LinkedHashMap<>(DEFAULTS);
            try {
                List<AppConfig> rows = appConfigMapper.selectAllConfig();
                for (AppConfig r : rows) {
                    String v = r.getCfgValue();
                    // 空值不回填：空串的语义是「用内置默认」，不是「覆盖成空」
                    if (v != null && !v.isBlank()) {
                        loaded.put(r.getCfgKey(), v);
                    } else if (!DEFAULTS.containsKey(r.getCfgKey())) {
                        loaded.put(r.getCfgKey(), v == null ? "" : v);
                    }
                }
            } catch (Exception e) {
                // 配置表读不到（比如还没执行 settings.sql）不该让整个查词挂掉，退回默认值
            }
            Map<String, String> snapshot = new HashMap<>(loaded);
            cache = snapshot;
            cachedAt = System.currentTimeMillis();
            return snapshot;
        }
    }

    private String checkSource(String label, String value, List<SettingsVO.Option> options) {
        if (value == null || value.isBlank()) {
            return "off";
        }
        boolean ok = options.stream().anyMatch(o -> o.getValue().equals(value));
        if (!ok) {
            String allowed = options.stream().map(SettingsVO.Option::getValue).collect(Collectors.joining("/"));
            throw new BusinessException(label + "源取值非法：" + value + "，允许 " + allowed);
        }
        return value;
    }

    private String checkUrl(String label, String url) {
        if (url == null || url.isBlank()) {
            return "";
        }
        String v = url.trim();
        if (!v.startsWith("http://") && !v.startsWith("https://")) {
            throw new BusinessException(label + "源地址必须以 http:// 或 https:// 开头");
        }
        if (v.length() > 400) {
            throw new BusinessException(label + "源地址过长");
        }
        return v;
    }

    private int clamp(Integer v, int min, int max, String label) {
        if (v == null) {
            return min;
        }
        if (v < min || v > max) {
            throw new BusinessException(label + "需在 " + min + " ~ " + max + " 之间");
        }
        return v;
    }

    private int parseInt(String s, int def) {
        try {
            return s == null || s.isBlank() ? def : Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}
