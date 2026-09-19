package com.cet6.sprint.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.cet6.sprint.entity.Article;
import com.cet6.sprint.vo.ArticleListVO;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 阅读文章 Mapper
 * <p>
 * 列表查询用 LEFT JOIN 而不是「先查文章再逐篇查进度」：
 * 后者在分页场景下是典型的 N+1（一页 12 篇就是 13 次查询）。
 * JOIN 的驱动表是 t_article，被驱动条件命中 uk_user_article 唯一索引，
 * 代价是每行一次索引查找。
 */
public interface ArticleMapper extends BaseMapper<Article> {

    /**
     * 文章列表（带当前用户阅读状态，支持分类 / 难度 / 关键词筛选）
     * <p>
     * 排序刻意用 a.id ASC 而不是发布时间：内容是按「分类内由易到难」编排入库的，
     * 按 id 排能保持这个教学顺序；按时间排会把 1865 年的美文甩到最后。
     */
    @Select("<script>"
            + "SELECT a.id, a.title, a.title_cn AS titleCn, a.category, a.source, a.author, "
            + "       a.difficulty, a.word_count AS wordCount, a.summary, a.publish_date AS publishDate, "
            + "       IFNULL(r.progress, 0) AS progress, "
            + "       IFNULL(r.finished, 0) AS finished, "
            + "       IFNULL(r.duration_sec, 0) AS durationSec "
            + "FROM t_article a "
            + "LEFT JOIN t_reading_record r ON r.article_id = a.id AND r.user_id = #{userId} "
            + "<where>"
            + "  <if test=\"category != null and category != ''\"> AND a.category = #{category} </if>"
            + "  <if test=\"difficulty != null\"> AND a.difficulty = #{difficulty} </if>"
            + "  <if test=\"unfinishedOnly != null and unfinishedOnly\"> AND IFNULL(r.finished, 0) = 0 </if>"
            + "  <if test=\"keyword != null and keyword != ''\">"
            + "    AND (a.title LIKE CONCAT('%', #{keyword}, '%') "
            + "         OR a.title_cn LIKE CONCAT('%', #{keyword}, '%') "
            + "         OR a.summary LIKE CONCAT('%', #{keyword}, '%'))"
            + "  </if>"
            + "</where>"
            + " ORDER BY a.id ASC"
            + "</script>")
    IPage<ArticleListVO> pageList(IPage<ArticleListVO> page,
                                  @Param("userId") Long userId,
                                  @Param("category") String category,
                                  @Param("difficulty") Integer difficulty,
                                  @Param("keyword") String keyword,
                                  @Param("unfinishedOnly") Boolean unfinishedOnly);

    /** 文章总数 */
    @Select("SELECT COUNT(*) FROM t_article")
    int totalCount();

    /** 用户已读完篇数 */
    @Select("SELECT COUNT(*) FROM t_reading_record WHERE user_id = #{userId} AND finished = 1")
    int finishedCount(@Param("userId") Long userId);

    /** 用户在读篇数（有进度但没读完） */
    @Select("SELECT COUNT(*) FROM t_reading_record "
            + "WHERE user_id = #{userId} AND finished = 0 AND progress > 0")
    int readingCount(@Param("userId") Long userId);

    /** 用户累计阅读秒数 */
    @Select("SELECT IFNULL(SUM(duration_sec), 0) FROM t_reading_record WHERE user_id = #{userId}")
    int totalDurationSec(@Param("userId") Long userId);

    /** 分类维度统计：每类文章数 + 已读完数 */
    @Select("SELECT a.category AS category, COUNT(*) AS total, "
            + "       SUM(CASE WHEN IFNULL(r.finished, 0) = 1 THEN 1 ELSE 0 END) AS finished "
            + "FROM t_article a "
            + "LEFT JOIN t_reading_record r ON r.article_id = a.id AND r.user_id = #{userId} "
            + "GROUP BY a.category")
    List<Map<String, Object>> statsByCategory(@Param("userId") Long userId);

    /**
     * 取用户读过的文章正文
     * <p>
     * 用于统计「已精读到的六级词」——把读过的文章正文取回来做并集。
     * 已读文章数量有限（学生一个月的精读量在几十篇量级），可以接受。
     */
    @Select("SELECT a.content FROM t_article a "
            + "JOIN t_reading_record r ON r.article_id = a.id "
            + "WHERE r.user_id = #{userId} AND r.progress > 0")
    List<String> readContents(@Param("userId") Long userId);
}
