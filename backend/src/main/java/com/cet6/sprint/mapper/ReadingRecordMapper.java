package com.cet6.sprint.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cet6.sprint.entity.ReadingRecord;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 阅读记录 Mapper
 * <p>
 * 两个写操作刻意分开：
 * · openOnce —— 打开文章时调用，只负责「打开次数 +1」
 * · saveProgress —— 进度上报时调用，只累计时长、推进进度
 * 如果把 read_count 也放进 saveProgress，一次阅读过程中前端每隔几秒上报
 * 一次，打开次数就会被算成几十次。
 */
public interface ReadingRecordMapper extends BaseMapper<ReadingRecord> {

    /** 记录一次打开（首次插入 read_count=1，重复打开则 +1） */
    @Insert("INSERT INTO t_reading_record (user_id, article_id, progress, duration_sec, finished, read_count) "
            + "VALUES (#{userId}, #{articleId}, 0, 0, 0, 1) "
            + "ON DUPLICATE KEY UPDATE read_count = read_count + 1")
    int openOnce(@Param("userId") Long userId, @Param("articleId") Long articleId);

    /**
     * 更新阅读进度与时长
     * progress 取 GREATEST —— 进度只进不退，重读不会把记录打回 0
     * duration_sec 累加 —— 同一篇反复读，时长应当累计
     */
    @Insert("INSERT INTO t_reading_record (user_id, article_id, progress, duration_sec, finished, read_count) "
            + "VALUES (#{userId}, #{articleId}, #{progress}, #{durationSec}, #{finished}, 0) "
            + "ON DUPLICATE KEY UPDATE "
            + "  progress = GREATEST(progress, #{progress}), "
            + "  duration_sec = duration_sec + #{durationSec}, "
            + "  finished = GREATEST(finished, #{finished})")
    int saveProgress(@Param("userId") Long userId,
                     @Param("articleId") Long articleId,
                     @Param("progress") Integer progress,
                     @Param("durationSec") Integer durationSec,
                     @Param("finished") Integer finished);

    /** 查询某用户对某篇文章的记录 */
    @Select("SELECT * FROM t_reading_record WHERE user_id = #{userId} AND article_id = #{articleId}")
    ReadingRecord find(@Param("userId") Long userId, @Param("articleId") Long articleId);

    /** 用户所有文章的累计阅读秒数（用于向学习日志结转分钟数） */
    @Select("SELECT IFNULL(SUM(duration_sec), 0) FROM t_reading_record WHERE user_id = #{userId}")
    int totalSeconds(@Param("userId") Long userId);
}
