package com.cet6.sprint.service.impl;

import com.cet6.sprint.external.ArpabetConverter;
import com.cet6.sprint.external.ExternalApiClient;
import com.cet6.sprint.service.ExternalSourceService;
import com.cet6.sprint.service.SettingsService;
import com.cet6.sprint.vo.ExternalDictVO;
import com.cet6.sprint.vo.SourceTestResultVO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * 外部数据源调度实现（词典 / 发音 / 翻译）
 * <p>
 * 各源的内置默认地址与解析逻辑都集中在这里。设置页把 baseUrl 留空时用默认，
 * 填了则整体替换 —— 这就是「换 API 源」的实现方式（可以指向自建中转服务）。
 * <p>
 * 已知上游限制（2026-09-14 实测，写进代码备忘）：
 * <ul>
 *   <li>MyMemory 单次 q 上限 500 字符，超出会返回「QUERY LENGTH LIMIT EXCEEDED」
 *       但 <b>HTTP 仍是 200</b>，必须按返回文本判断成败；</li>
 *   <li>Datamuse 会返回拼写相近的词（如查 ubiquitous 也会返回 ubiquitious），
 *       必须优先取 word 完全相等的那条，否则会张冠李戴。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExternalSourceServiceImpl implements ExternalSourceService {

    // ---- 内置默认地址 ----
    private static final String DEF_DICT_DATAMUSE = "https://api.datamuse.com/words";
    private static final String DEF_DICT_FREEDICT = "https://api.dictionaryapi.dev/api/v2/entries/en/";
    private static final String DEF_PRON_YOUDAO = "https://dict.youdao.com/dictvoice";
    private static final String DEF_TRANSLATE_MYMEMORY = "https://api.mymemory.translated.net/get";

    /** MyMemory 硬上限 500，留 10% 余量按 450 切分 */
    private static final int TRANSLATE_CHUNK = 450;

    private static final int MAX_DEFINITIONS = 5;

    private final SettingsService settingsService;
    private final ExternalApiClient client;
    private final ObjectMapper objectMapper;

    // ============================================================
    // 词典
    // ============================================================

    @Override
    public ExternalDictVO lookupDict(String word) {
        if (!externalEnabled()) {
            return null;
        }
        String source = settingsService.value(SettingsService.Keys.DICT_SOURCE, "datamuse");
        if ("off".equals(source) || word == null || word.isBlank()) {
            return null;
        }
        try {
            return "dictionaryapi".equals(source) ? fromFreeDictionary(word) : fromDatamuse(word);
        } catch (Exception e) {
            log.warn("[词典源 {}] 解析 {} 失败: {}", source, word, e.getMessage());
            return null;
        }
    }

    /** Datamuse：tags 里带词性与 ARPAbet 发音，defs 里是按词性分组的释义 */
    private ExternalDictVO fromDatamuse(String word) throws Exception {
        String base = orDefault(settingsService.value(SettingsService.Keys.DICT_BASE_URL, ""), DEF_DICT_DATAMUSE);
        String url = base + "?sp=" + enc(word) + "&md=dpr&max=3";
        String body = client.get(url, timeout(), cacheMinutes());
        if (body == null) {
            return null;
        }
        JsonNode arr = objectMapper.readTree(body);
        if (!arr.isArray() || arr.isEmpty()) {
            return null;
        }

        // 优先取拼写完全一致的那条，避免拿到 Datamuse 给出的相近词
        JsonNode hit = null;
        for (JsonNode n : arr) {
            if (word.equalsIgnoreCase(n.path("word").asText(""))) {
                hit = n;
                break;
            }
        }
        if (hit == null) {
            // 没有完全匹配说明该词不在 Datamuse 词表里（多为拼写错误），不硬凑
            return null;
        }

        ExternalDictVO vo = new ExternalDictVO();
        vo.setSource("datamuse");

        Set<String> posSet = new LinkedHashSet<>();
        for (JsonNode t : hit.path("tags")) {
            String tag = t.asText("");
            if (tag.startsWith("pron:")) {
                String ipa = ArpabetConverter.toIpa(tag.substring(5));
                if (!ipa.equals("/")) {
                    vo.setPhonetic(ipa);
                }
            } else if (!tag.isBlank()) {
                posSet.add(ArpabetConverter.posAbbr(tag));
            }
        }
        vo.setPos(String.join(" / ", posSet));

        List<String> defs = new ArrayList<>();
        for (JsonNode d : hit.path("defs")) {
            // 形如 "n\tHello!\" or an equivalent greeting."
            String text = d.asText("");
            int tab = text.indexOf('\t');
            String content = tab >= 0 ? text.substring(tab + 1) : text;
            content = content.trim();
            if (!content.isEmpty()) {
                defs.add(content);
            }
            if (defs.size() >= MAX_DEFINITIONS) {
                break;
            }
        }
        vo.setDefinitions(defs);
        return vo;
    }

    /** Free Dictionary API：phonetic / phonetics[].audio / meanings[] */
    private ExternalDictVO fromFreeDictionary(String word) throws Exception {
        String base = orDefault(settingsService.value(SettingsService.Keys.DICT_BASE_URL, ""), DEF_DICT_FREEDICT);
        String url = base.endsWith("/") ? base + enc(word) : base + "/" + enc(word);
        String body = client.get(url, timeout(), cacheMinutes());
        if (body == null) {
            return null;
        }
        JsonNode arr = objectMapper.readTree(body);
        if (!arr.isArray() || arr.isEmpty()) {
            return null;
        }
        JsonNode e0 = arr.get(0);

        ExternalDictVO vo = new ExternalDictVO();
        vo.setSource("dictionaryapi");
        String phonetic = e0.path("phonetic").asText("");
        String audio = "";
        for (JsonNode p : e0.path("phonetics")) {
            if (phonetic.isBlank() && !p.path("text").asText("").isBlank()) {
                phonetic = p.path("text").asText("");
            }
            String a = p.path("audio").asText("");
            if (audio.isBlank() && !a.isBlank()) {
                audio = a;
            }
        }
        vo.setPhonetic(phonetic);
        if (!audio.isBlank()) {
            vo.setAudioUrl(audio);
            vo.setPronSource("dictionaryapi");
        }

        Set<String> posSet = new LinkedHashSet<>();
        List<String> defs = new ArrayList<>();
        for (JsonNode m : e0.path("meanings")) {
            String pos = m.path("partOfSpeech").asText("");
            if (!pos.isBlank()) {
                posSet.add(pos + ".");
            }
            for (JsonNode d : m.path("definitions")) {
                String text = d.path("definition").asText("").trim();
                if (!text.isEmpty() && defs.size() < MAX_DEFINITIONS) {
                    defs.add(text);
                }
            }
        }
        vo.setPos(String.join(" / ", posSet));
        vo.setDefinitions(defs);
        return vo;
    }

    // ============================================================
    // 发音
    // ============================================================

    @Override
    public String pronSource() {
        if (!externalEnabled()) {
            return "off";
        }
        return settingsService.value(SettingsService.Keys.PRON_SOURCE, "youdao");
    }

    @Override
    public String pronUrl(String word) {
        if (!externalEnabled() || word == null || word.isBlank()) {
            return null;
        }
        String source = settingsService.value(SettingsService.Keys.PRON_SOURCE, "youdao");
        if ("off".equals(source)) {
            return null;
        }
        if ("youdao".equals(source)) {
            // 有道发音是「URL 即音频」，不需要先请求再取地址，直接拼出来给前端播放
            String base = orDefault(settingsService.value(SettingsService.Keys.PRON_BASE_URL, ""), DEF_PRON_YOUDAO);
            return base + "?audio=" + enc(word) + "&type=2";
        }
        if ("dictionaryapi".equals(source)) {
            ExternalDictVO d = fromFreeDictionaryQuiet(word);
            return d == null ? null : d.getAudioUrl();
        }
        return null;
    }

    private ExternalDictVO fromFreeDictionaryQuiet(String word) {
        try {
            return fromFreeDictionary(word);
        } catch (Exception e) {
            return null;
        }
    }

    // ============================================================
    // 翻译
    // ============================================================

    @Override
    public TranslateOutcome translateZh2En(String zhText) {
        if (!externalEnabled() || zhText == null || zhText.isBlank()) {
            return null;
        }
        String source = settingsService.value(SettingsService.Keys.TRANSLATE_SOURCE, "mymemory");
        if ("off".equals(source)) {
            return null;
        }
        if (!"mymemory".equals(source)) {
            return null;
        }

        List<String> chunks = chunk(zhText.trim(), TRANSLATE_CHUNK);
        if (chunks.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        int ok = 0;
        for (String c : chunks) {
            String t = myMemory(c);
            if (t == null) {
                continue;
            }
            if (sb.length() > 0) {
                sb.append(' ');
            }
            sb.append(t);
            ok++;
        }
        if (ok == 0) {
            return null;
        }
        return new TranslateOutcome(sb.toString(), "mymemory", ok == chunks.size(), chunks.size());
    }

    private String myMemory(String text) {
        String base = orDefault(settingsService.value(SettingsService.Keys.TRANSLATE_BASE_URL, ""),
                DEF_TRANSLATE_MYMEMORY);
        String url = base + "?q=" + enc(text) + "&langpair=" + enc("zh-CN|en");
        String body = client.get(url, timeout(), cacheMinutes());
        if (body == null) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            String t = root.path("responseData").path("translatedText").asText("");
            return isUpstreamError(t) ? null : t;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * MyMemory 的错误是以「正常 200 + 错误文案」的形式返回的，必须按文本识别。
     * 这段判断是整个翻译链路里最容易踩的坑，单独抽出来并写清注释。
     */
    private boolean isUpstreamError(String t) {
        if (t == null || t.isBlank()) {
            return true;
        }
        String u = t.toUpperCase();
        return u.contains("QUERY LENGTH LIMIT EXCEEDED")
                || u.contains("NO QUERY SPECIFIED")
                || u.contains("INVALID TARGET LANGUAGE")
                || u.contains("INVALID SOURCE LANGUAGE")
                || u.contains("MYMEMORY WARNING")
                || u.contains("PLEASE SELECT TWO DISTINCT LANGUAGES");
    }

    /**
     * 按上限把中文切成若干段。
     * <p>
     * 优先在句末标点处断句（译文更连贯），单句仍然超长时才硬切。
     */
    private List<String> chunk(String text, int limit) {
        List<String> out = new ArrayList<>();
        StringBuilder buf = new StringBuilder();
        for (String seg : text.split("(?<=[。！？；!?;\\n])")) {
            if (seg.isEmpty()) {
                continue;
            }
            if (seg.length() > limit) {
                if (buf.length() > 0) {
                    out.add(buf.toString().trim());
                    buf.setLength(0);
                }
                for (int i = 0; i < seg.length(); i += limit) {
                    out.add(seg.substring(i, Math.min(seg.length(), i + limit)).trim());
                }
                continue;
            }
            if (buf.length() + seg.length() > limit && buf.length() > 0) {
                out.add(buf.toString().trim());
                buf.setLength(0);
            }
            buf.append(seg);
        }
        if (buf.length() > 0) {
            out.add(buf.toString().trim());
        }
        out.removeIf(String::isBlank);
        return out;
    }

    // ============================================================
    // 连通性测试
    // ============================================================

    @Override
    public SourceTestResultVO test(String type) {
        SourceTestResultVO vo = new SourceTestResultVO();
        vo.setType(type);
        long t0 = System.currentTimeMillis();
        try {
            if ("dict".equals(type)) {
                String src = settingsService.value(SettingsService.Keys.DICT_SOURCE, "datamuse");
                vo.setSource(src);
                if ("off".equals(src)) {
                    return fail(vo, "该源已关闭，未发起请求");
                }
                ExternalDictVO d = lookupDict("resilience");
                if (d == null) {
                    return fail(vo, "无返回（源站不可用或该词未收录）");
                }
                vo.setOk(true);
                vo.setMessage(d.getSource() + " 可用 · 音标 " + nz(d.getPhonetic())
                        + (d.getDefinitions() == null || d.getDefinitions().isEmpty()
                        ? "" : " · 释义 " + d.getDefinitions().get(0)));
                return vo;
            }
            if ("pron".equals(type)) {
                String src = settingsService.value(SettingsService.Keys.PRON_SOURCE, "youdao");
                vo.setSource(src);
                if ("off".equals(src)) {
                    return fail(vo, "该源已关闭，未发起请求");
                }
                String url = pronUrl("resilience");
                if (url == null) {
                    return fail(vo, "取不到发音地址");
                }
                boolean ok = client.reachable(url, timeout());
                vo.setOk(ok);
                vo.setMessage(ok ? src + " 可用 · " + url : "地址不可达：" + url);
                return vo;
            }
            if ("translate".equals(type)) {
                String src = settingsService.value(SettingsService.Keys.TRANSLATE_SOURCE, "mymemory");
                vo.setSource(src);
                if ("off".equals(src)) {
                    return fail(vo, "该源已关闭，未发起请求");
                }
                TranslateOutcome r = translateZh2En("考试加油。");
                if (r == null) {
                    return fail(vo, "无返回（源站不可用或触发限流）");
                }
                vo.setOk(true);
                vo.setMessage(r.getEngine() + " 可用 · " + r.getText());
                return vo;
            }
            return fail(vo, "未知的测试类型：" + type);
        } finally {
            vo.setCostMs(System.currentTimeMillis() - t0);
        }
    }

    private SourceTestResultVO fail(SourceTestResultVO vo, String msg) {
        vo.setOk(false);
        vo.setMessage(msg);
        return vo;
    }

    // ============================================================
    // 工具
    // ============================================================

    private boolean externalEnabled() {
        return !"0".equals(settingsService.value(SettingsService.Keys.ENABLED, "1"));
    }

    private int timeout() {
        return settingsService.intValue(SettingsService.Keys.TIMEOUT_MS, 6000);
    }

    private int cacheMinutes() {
        return settingsService.intValue(SettingsService.Keys.CACHE_MINUTES, 10);
    }

    private String orDefault(String v, String def) {
        return v == null || v.isBlank() ? def : v.trim();
    }

    private String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private String nz(String s) {
        return s == null ? "" : s;
    }
}
