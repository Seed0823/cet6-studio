package com.cet6.sprint.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cet6.sprint.entity.WordGrade;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 单词掌握度 Mapper
 */
public interface WordGradeMapper extends BaseMapper<WordGrade> {

    /**
     * 提交掌握度：同一用户同一单词只保留一行，重复提交则更新等级并累计复习次数
     */
    @Insert("INSERT INTO t_word_grade (user_id, word_id, grade, review_count, next_review) "
            + "VALUES (#{userId}, #{wordId}, #{grade}, 1, #{nextReview}) "
            + "ON DUPLICATE KEY UPDATE grade = #{grade}, review_count = review_count + 1, "
            + "next_review = #{nextReview}")
    int upsertGrade(@Param("userId") Long userId,
                    @Param("wordId") Long wordId,
                    @Param("grade") Integer grade,
                    @Param("nextReview") java.time.LocalDate nextReview);

    /** 待复习数量 */
    @Select("SELECT COUNT(*) FROM t_word_grade WHERE user_id = #{userId} "
            + "AND next_review <= CURDATE() AND grade < 2")
    int countDueReview(@Param("userId") Long userId);

    /** 已掌握数量（grade = 2） */
    @Select("SELECT COUNT(*) FROM t_word_grade WHERE user_id = #{userId} AND grade = 2")
    int countKnown(@Param("userId") Long userId);

    /** 已学过的词总数 */
    @Select("SELECT COUNT(*) FROM t_word_grade WHERE user_id = #{userId}")
    int countLearned(@Param("userId") Long userId);
}
