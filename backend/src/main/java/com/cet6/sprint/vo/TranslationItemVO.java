package com.cet6.sprint.vo;

import lombok.Data;

/**
 * 翻译练习题目（练习态：不含参考译文与关键点，提交后由 ResultVO 返回）
 */
@Data
public class TranslationItemVO {

    private Long id;
    private String category;
    private String title;
    private String chineseText;
    private Integer difficulty;
}
