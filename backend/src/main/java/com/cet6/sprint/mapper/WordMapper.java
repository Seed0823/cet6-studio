package com.cet6.sprint.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cet6.sprint.entity.Word;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 词库 Mapper
 */
public interface WordMapper extends BaseMapper<Word> {

    /**
     * 随机取 N 个六级词（已掌握 / 已学过的词优先排除）
     * <p>
     * 用 NOT EXISTS 而不是 NOT IN：NOT IN 遇到子查询含 NULL 会整体返回空集，
     * 且数据量大时 NOT EXISTS 通常走半连接，效率更好。
     */
    @Select("SELECT w.* FROM t_word w "
            + "WHERE w.is_cet6 = 1 "
            + "AND NOT EXISTS (SELECT 1 FROM t_word_grade g WHERE g.word_id = w.id AND g.user_id = #{userId}) "
            + "ORDER BY RAND() LIMIT #{limit}")
    List<Word> randomNewCet6(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 随机取 N 个六级词（不排除已学，用于复习 / 出题兜底）
     */
    @Select("SELECT w.* FROM t_word w WHERE w.is_cet6 = 1 ORDER BY RAND() LIMIT #{limit}")
    List<Word> randomCet6(@Param("limit") int limit);

    /**
     * 取被当作干扰项的词（同难度：柯林斯星级接近）
     */
    @Select("SELECT w.* FROM t_word w WHERE w.is_cet6 = 1 AND w.id <> #{excludeId} "
            + "ORDER BY RAND() LIMIT #{limit}")
    List<Word> randomDistractors(@Param("excludeId") Long excludeId, @Param("limit") int limit);

    /**
     * 取今天到期的复习词（艾宾浩斯）
     */
    @Select("SELECT w.* FROM t_word w JOIN t_word_grade g ON g.word_id = w.id "
            + "WHERE g.user_id = #{userId} AND g.next_review <= CURDATE() AND g.grade < 2 "
            + "ORDER BY g.next_review ASC LIMIT #{limit}")
    List<Word> findDueReview(@Param("userId") Long userId, @Param("limit") int limit);

    /**
     * 查词自动补全：前缀匹配候选词
     * <p>
     * 三个设计要点：
     * 1. 只传 pattern（如 "all%"），不用 CONCAT 拼接 —— 前缀 LIKE 能直接走 uk_word 唯一索引的范围扫描，
     * 5.7 万行也是毫秒级；写成 '%all%' 就退化成全表扫描了。
     * 2. 排序 is_cet6 DESC 优先 —— 备考场景下，六级大纲词比生僻词更有参考价值。
     * 3. frq 用 COALESCE(NULLIF(frq,0),999999) 兜底 —— frq=0 表示「无词频数据」而非「最高频」，
     * 直接 ORDER BY frq ASC 会让这些词排到最前面，是常见坑。
     */
    @Select("SELECT w.* FROM t_word w "
            + "WHERE w.word LIKE #{pattern} "
            + "ORDER BY w.is_cet6 DESC, COALESCE(NULLIF(w.frq, 0), 999999) ASC, w.word ASC "
            + "LIMIT #{limit}")
    List<Word> suggestByPrefix(@Param("pattern") String pattern, @Param("limit") int limit);

    /**
     * 取全部六级大纲词（只取 word 列）
     * <p>
     * 供阅读模块在启动时把 5000 多个词装进内存 Set，之后对文章正文分词就能
     * O(1) 判断某个词是不是六级词，不必每篇文章去 JOIN 一次词库。
     * 只查 word 一列是因为 Set 里只需要拼写，把 phonetic/translation 一起拉回来
     * 会让首屏多传几 MB 无用数据。
     */
    @Select("SELECT word FROM t_word WHERE is_cet6 = 1")
    List<String> listCet6Words();
}
