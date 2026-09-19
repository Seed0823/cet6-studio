package com.cet6.sprint.external;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ARPAbet → IPA 音标转换
 * <p>
 * Datamuse 的音标是 CMU 发音词典的 ARPAbet 格式（如 <code>HH AH0 L OW1</code>），
 * 直接展示给用户没有意义；这里转成 IPA（<code>/həˈloʊ/</code>）。
 * <p>
 * 映射关系是 CMUdict 与 IPA 的标准对照。少数音素在英美音上有分歧
 * （如 AA 记作 ɑ 还是 ɒ），这里取美式常见写法；未收录的音素直接跳过而不报错，
 * 保证最坏情况下也能还原出大部分音标。
 */
public final class ArpabetConverter {

    private ArpabetConverter() {
    }

    public static String toIpa(String arpabet) {
        if (arpabet == null || arpabet.isBlank()) {
            return "";
        }
        StringBuilder sb = new StringBuilder("/");
        // 待定辅音缓冲（按音素存，便于按「合法节首」切分）。
        // IPA 里重音符号标在「重读音节的开头」，即该音节起始辅音之前，
        // 所以遇到辅音先攒着，等重读元音进来再决定从哪里切开。
        //
        // 关键：不能把所有前面的辅音都划给重读音节。例如 ambiguous 的
        // AE M B IH1，若把 m b 都给 IH，会得到 /æˈmbɪɡjuəs/（错），
        // 标准是 /æmˈbɪɡjuəs/ —— 因为 /mb/ 不是合法的英语节首，m 必须留在前一个音节。
        List<String> pending = new ArrayList<>();
        for (String raw : arpabet.trim().split("\\s+")) {
            String token = raw.trim();
            if (token.isEmpty()) {
                continue;
            }
            int stress = -1;
            char last = token.charAt(token.length() - 1);
            if (last >= '0' && last <= '9') {
                stress = last - '0';
                token = token.substring(0, token.length() - 1);
            }
            String base = token.toUpperCase();
            String ipa = map(base, stress);
            if (ipa.isEmpty()) {
                continue;
            }
            if (!isVowel(base)) {
                pending.add(ipa);
                continue;
            }

            // 元音：先决定末尾有多少个辅音可以作为本次的节首
            int take = onsetLength(pending);
            int keep = pending.size() - take;
            for (int i = 0; i < keep; i++) {
                sb.append(pending.get(i));
            }
            if (stress == 1) {
                sb.append('ˈ');
            } else if (stress == 2) {
                sb.append('ˌ');
            }
            for (int i = keep; i < pending.size(); i++) {
                sb.append(pending.get(i));
            }
            pending.clear();
            sb.append(ipa);
        }
        for (String c : pending) {
            sb.append(c);
        }
        sb.append('/');
        return sb.toString();
    }

    /**
     * 从缓冲末尾往回数，取最长的「合法英语节首」音素数（最多 3 个）。
     * <p>
     * 这就是最大节首原则（maximal onset principle）：辅音尽量划给后一个音节，
     * 但不能构成非法节首。找不到合法组合时返回 0，重音符号紧贴元音。
     */
    private static int onsetLength(List<String> pending) {
        int max = Math.min(pending.size(), 3);
        for (int k = max; k >= 1; k--) {
            StringBuilder cand = new StringBuilder();
            for (int i = pending.size() - k; i < pending.size(); i++) {
                cand.append(pending.get(i));
            }
            if (isLegalOnset(cand.toString())) {
                return k;
            }
        }
        return 0;
    }

    /** 合法节首：单个辅音一律允许；多辅音需在英语常见的 onset 簇表里 */
    private static boolean isLegalOnset(String c) {
        if (c.length() <= 1) {
            return true;
        }
        return ONSET2.contains(c) || ONSET3.contains(c);
    }

    /** 双辅音节首（用本类输出的 IPA 字符表示） */
    private static final Set<String> ONSET2 = new HashSet<>(Arrays.asList(
            "pɹ", "bɹ", "tɹ", "dɹ", "kɹ", "ɡɹ", "fɹ", "θɹ", "ʃɹ",
            "pl", "bl", "kl", "ɡl", "fl", "sl",
            "sp", "st", "sk", "sm", "sn", "sw",
            "tw", "dw", "kw", "ɡw",
            "pj", "bj", "kj", "ɡj", "fj", "vj", "hj", "mj", "nj", "lj"
    ));

    /** 三辅音节首 */
    private static final Set<String> ONSET3 = new HashSet<>(Arrays.asList(
            "spɹ", "stɹ", "skɹ", "skw", "spl", "spj", "stj", "skj"
    ));

    /** 元音音素集合 —— 用来判断重音符号该不该插在这个音素前面 */
    private static boolean isVowel(String p) {
        switch (p) {
            case "AA":
            case "AE":
            case "AH":
            case "AO":
            case "AW":
            case "AY":
            case "EH":
            case "ER":
            case "EY":
            case "IH":
            case "IY":
            case "OW":
            case "OY":
            case "UH":
            case "UW":
                return true;
            default:
                return false;
        }
    }

    private static String map(String p, int stress) {
        switch (p) {
            // ---- 元音 ----
            case "AA": return "ɑ";
            case "AE": return "æ";
            // AH 在非重读时是央元音 ə，重读时是 ʌ
            case "AH": return stress == 0 ? "ə" : "ʌ";
            case "AO": return "ɔ";
            case "AW": return "aʊ";
            case "AY": return "aɪ";
            case "EH": return "ɛ";
            case "ER": return stress == 0 ? "ɚ" : "ɝ";
            case "EY": return "eɪ";
            case "IH": return "ɪ";
            case "IY": return "i";
            case "OW": return "oʊ";
            case "OY": return "ɔɪ";
            case "UH": return "ʊ";
            case "UW": return "u";
            // ---- 辅音 ----
            case "B": return "b";
            case "CH": return "tʃ";
            case "D": return "d";
            case "DH": return "ð";
            case "F": return "f";
            case "G": return "ɡ";
            case "HH": return "h";
            case "JH": return "dʒ";
            case "K": return "k";
            case "L": return "l";
            case "M": return "m";
            case "N": return "n";
            case "NG": return "ŋ";
            case "P": return "p";
            case "R": return "ɹ";
            case "S": return "s";
            case "SH": return "ʃ";
            case "T": return "t";
            case "TH": return "θ";
            case "V": return "v";
            case "W": return "w";
            case "Y": return "j";
            case "Z": return "z";
            case "ZH": return "ʒ";
            default: return "";
        }
    }

    /** ARPAbet 里的词性标记 → 常见缩写 */
    public static String posAbbr(String tag) {
        switch (tag) {
            case "n": return "n.";
            case "v": return "v.";
            case "adj": return "adj.";
            case "adv": return "adv.";
            case "prop": return "专有";
            case "u": return "其他";
            default: return tag;
        }
    }
}
