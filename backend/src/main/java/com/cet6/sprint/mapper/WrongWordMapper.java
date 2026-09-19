package com.cet6.sprint.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cet6.sprint.entity.WrongWord;
import com.cet6.sprint.vo.WrongWordVO;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 错词本 Mapper
 */
public interface WrongWordMapper extends BaseMapper<WrongWord> {

    /**
     * 记入错词本：已存在则答错次数 +1 并重置为「未攻克」
     */
    @Insert("INSERT INTO t_wrong_word (user_id, word_id, source, wrong_count) "
            + "VALUES (#{userId}, #{wordId}, #{source}, 1) "
            + "ON DUPLICATE KEY UPDATE wrong_count = wrong_count + 1, mastered = 0")
    int upsertWrong(@Param("userId") Long userId,
                    @Param("wordId") Long wordId,
                    @Param("source") String source);

    /**
     * 错词本列表（关联词库取释义）
     * mastered 传 null 表示不过滤
     */
    @Select("<script>"
            + "SELECT ww.id AS wrongId, ww.source, ww.wrong_count AS wrongCount, "
            + "       ww.mastered, ww.create_time AS createTime, "
            + "       w.id AS wordId, w.word, w.phonetic, w.pos, w.translation "
            + "FROM t_wrong_word ww "
            + "JOIN t_word w ON w.id = ww.word_id "
            + "WHERE ww.user_id = #{userId} "
            + "<if test='mastered != null'> AND ww.mastered = #{mastered} </if>"
            + "ORDER BY ww.create_time DESC"
            + "</script>")
    List<WrongWordVO> listWithWord(@Param("userId") Long userId,
                                   @Param("mastered") Integer mastered);

    @Select("SELECT COUNT(*) FROM t_wrong_word WHERE user_id = #{userId} AND mastered = 0")
    int countUnmastered(@Param("userId") Long userId);
}
