package com.cet6.sprint.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 阅读记录（进度 + 时长）
 * <p>
 * 按 (user_id, article_id) 唯一，靠 ON DUPLICATE KEY UPDATE 做 upsert。
 * 两个字段的更新策略刻意不同：
 * · duration_sec 累加 —— 同一篇文章反复读，时长应当累计
 * · progress 取最大值 —— 进度只进不退，重读时不该把进度打回去
 */
@Data
@TableName("t_reading_record")
public class ReadingRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long articleId;

    /** 阅读进度百分比 0-100 */
    private Integer progress;

    /** 累计阅读时长（秒） */
    private Integer durationSec;

    /** 0 在读 1 已读完 */
    private Integer finished;

    /** 打开次数 */
    private Integer readCount;

    private LocalDateTime firstTime;

    private LocalDateTime lastTime;
}
