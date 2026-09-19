package com.cet6.sprint.external;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 外部 HTTP 客户端（统一出口）
 * <p>
 * 为什么用 JDK17 自带的 {@link HttpClient} 而不是 RestTemplate/WebClient：
 * <ul>
 *   <li>RestTemplate 的超时绑定在 RequestFactory 上，改超时就得重建对象，
 *       而本项目超时是可配置的（ext.timeoutMs），用 HttpClient 可以每次请求单独设 timeout；</li>
 *   <li>WebClient 需要额外引 spring-webflux，为一个 GET 引一整个响应式栈不划算。</li>
 * </ul>
 * 客户端本身是单例复用的（内含连接池），只有超时随请求走。
 * <p>
 * 三个职责：超时保护、失败重试一次、结果缓存 —— 免费接口有额度限制，
 * 缓存能显著减少重复调用（同一个词查两遍不该花两次额度）。
 */
@Slf4j
@Component
public class ExternalApiClient {

    /** 默认 UA：部分站点对空 UA 直接拒绝 */
    private static final String UA = "Mozilla/5.0 (compatible; CET6Studio/1.0)";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    /** url -> 缓存条目 */
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    /**
     * 失败熔断：host -> 恢复时间戳。
     * <p>
     * 为什么需要它：如果源站整体挂掉（实测 dictionaryapi 就出现过后端 522），
     * 每个从没查过的词都要等满「超时 × 重试次数」才失败，查词会卡到没法用。
     * 记下这个 host 最近失败过，短时间内直接短路返回 null，把等待时间降到 0。
     * 只熔断 60 秒，恢复后自动重试，不会永久屏蔽一个源。
     */
    private final Map<String, Long> failUntil = new ConcurrentHashMap<>();

    private static final long FAIL_COOLDOWN_MS = 60_000L;

    private static final class CacheEntry {
        final String value;
        final long expireAt;

        CacheEntry(String value, long expireAt) {
            this.value = value;
            this.expireAt = expireAt;
        }
    }

    /**
     * GET 并返回响应体字符串。
     *
     * @param url          完整地址
     * @param timeoutMs    单次请求超时
     * @param cacheMinutes 缓存分钟数，≤0 表示不缓存
     * @return 响应体；失败返回 null（调用方负责降级，这里不抛异常）
     */
    public String get(String url, int timeoutMs, int cacheMinutes) {
        if (url == null || url.isBlank()) {
            return null;
        }
        long now = System.currentTimeMillis();
        if (cacheMinutes > 0) {
            CacheEntry hit = cache.get(url);
            if (hit != null && hit.expireAt > now) {
                return hit.value;
            }
        }

        String host = hostOf(url);
        Long until = failUntil.get(host);
        if (until != null && until > now) {
            // 该源刚失败过，本轮直接短路，不再等待超时
            return null;
        }

        String body = request(url, Math.max(timeoutMs, 1000));
        if (body == null) {
            // 失败重试一次：免费接口偶发抖动
            body = request(url, Math.max(timeoutMs, 1000));
        }

        if (body == null) {
            failUntil.put(host, now + FAIL_COOLDOWN_MS);
            return null;
        }
        failUntil.remove(host);
        if (cacheMinutes > 0) {
            pruneIfNeeded(now);
            cache.put(url, new CacheEntry(body, now + cacheMinutes * 60_000L));
        }
        return body;
    }

    /** 轻量探活：只关心能否拿到 2xx，不需要正文 */
    public boolean reachable(String url, int timeoutMs) {
        return request(url, Math.max(timeoutMs, 1000)) != null;
    }

    /** 配置变更后清空缓存，避免还返回旧源的数据 */
    public void clearCache() {
        cache.clear();
        failUntil.clear();
    }

    public int cacheSize() {
        return cache.size();
    }

    /** 取 host 作为熔断键：同源站不同路径共用一个熔断状态 */
    private String hostOf(String url) {
        try {
            return URI.create(url).getHost();
        } catch (Exception e) {
            return url;
        }
    }

    private String request(String url, int timeoutMs) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create(url))
                    .timeout(Duration.ofMillis(timeoutMs))
                    .header("User-Agent", UA)
                    .header("Accept", "*/*")
                    .GET()
                    .build();
            HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            int sc = resp.statusCode();
            if (sc >= 200 && sc < 300) {
                return resp.body();
            }
            log.warn("[外部源] {} -> HTTP {}", url, sc);
            return null;
        } catch (Exception e) {
            log.warn("[外部源] {} 调用失败: {}", url, e.getClass().getSimpleName() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * 简单清理：条目数超过阈值时，顺手清掉已过期的。
     * 不做定时任务 —— 配置项很少、流量不大，惰性清理足够，也少一个线程。
     */
    private void pruneIfNeeded(long now) {
        if (cache.size() < 512) {
            return;
        }
        cache.entrySet().removeIf(e -> e.getValue().expireAt <= now);
    }
}
