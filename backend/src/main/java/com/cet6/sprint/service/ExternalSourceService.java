package com.cet6.sprint.service;

import com.cet6.sprint.vo.ExternalDictVO;
import com.cet6.sprint.vo.SourceTestResultVO;

/**
 * 外部数据源调度（词典 / 发音 / 翻译）
 * <p>
 * 具体用哪个源由设置页决定，这里只做「读配置 → 选源 → 调用 → 解析」。
 * 所有方法在外部源不可用时都返回 null 或带 note 的对象，**不抛异常**——
 * 在线源是增强，不该拖垮本地词库这条主链路。
 */
public interface ExternalSourceService {

    /** 词典增强：音标 + 词性 + 英文释义。取不到返回 null */
    ExternalDictVO lookupDict(String word);

    /** 发音 mp3 直链。关闭或取不到返回 null（前端回退浏览器 TTS） */
    String pronUrl(String word);

    /** 当前生效的发音源标识，供前端标注「发音来自哪里」；关闭时返回 off */
    String pronSource();

    /** 中译英（内部按上游 500 字符上限自动分段）。取不到返回 null */
    TranslateOutcome translateZh2En(String zhText);

    /** 连通性测试，type ∈ {dict, pron, translate} */
    SourceTestResultVO test(String type);

    /** 翻译结果（含是否分段完整） */
    class TranslateOutcome {
        private final String text;
        private final String engine;
        private final boolean complete;
        private final int segments;

        public TranslateOutcome(String text, String engine, boolean complete, int segments) {
            this.text = text;
            this.engine = engine;
            this.complete = complete;
            this.segments = segments;
        }

        public String getText() {
            return text;
        }

        public String getEngine() {
            return engine;
        }

        public boolean isComplete() {
            return complete;
        }

        public int getSegments() {
            return segments;
        }
    }
}
