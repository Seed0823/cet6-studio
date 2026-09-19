package com.cet6.sprint.vo;

import lombok.Data;

import java.util.List;

/**
 * 外部词典源返回的增强信息
 * <p>
 * 只承载「本地 ECDICT 没有或不如在线源」的部分：音标、词性、英文释义、发音直链。
 * 中文释义仍以本地词库为准，因为在线源的中文质量不稳定。
 */
@Data
public class ExternalDictVO {

    /** 命中的词典源标识：datamuse / dictionaryapi */
    private String source;

    /** 音标。Datamuse 返回 ARPAbet（HH AH0 L OW1），dictionaryapi 返回 IPA */
    private String phonetic;

    /** 词性，多值用 / 连接 */
    private String pos;

    /** 英文释义（最多取若干条，避免前端过长） */
    private List<String> definitions;

    /** 发音 mp3 直链 */
    private String audioUrl;

    /** 发音来源标识：youdao / dictionaryapi */
    private String pronSource;

    /** 降级说明：外部源不可用时给出原因，前端据此决定是否提示 */
    private String note;
}
