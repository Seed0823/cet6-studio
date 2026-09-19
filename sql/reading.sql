-- =====================================================================
-- CET6 Studio · 悦读模块 · 建表脚本
-- MySQL 8.0+ / utf8mb4
-- 执行：mysql -uroot -proot -e "source D:/WorkBuddyFiles/outputs/cet6-studio/sql/reading.sql"
-- =====================================================================

USE cet6_sprint;

-- ---------------------------------------------------------------------
-- 1. 文章主表
--    设计要点：
--    · content / translation 都按「段」存储，段落之间用空行（\n\n）分隔，
--      两列段落数严格一一对应，前端据此做译文对照（逐段对齐而不是整篇并排）
--    · 不存 cet6_words 列：六级词由后端在内存中缓存词表后按需计算，
--      避免导入期依赖，改词表也不用重跑导入
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_article;
CREATE TABLE t_article (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  title         VARCHAR(200) NOT NULL                COMMENT '英文标题',
  title_cn      VARCHAR(200) DEFAULT NULL            COMMENT '中文标题',
  category      VARCHAR(20)  NOT NULL                COMMENT '分类：exam真题仿真 / essay公版美文 / news外刊新闻 / science科普短文',
  source        VARCHAR(120) DEFAULT NULL            COMMENT '来源说明',
  source_url    VARCHAR(500) DEFAULT NULL            COMMENT '原文链接（公版书/新闻源）',
  author        VARCHAR(80)  DEFAULT NULL            COMMENT '作者',
  difficulty    TINYINT      DEFAULT 3               COMMENT '难度 1-5',
  word_count    INT          DEFAULT 0               COMMENT '英文词数',
  summary       VARCHAR(300) DEFAULT NULL            COMMENT '中文简介（列表页展示）',
  content       MEDIUMTEXT                           COMMENT '英文正文（段落以空行分隔）',
  translation   MEDIUMTEXT                           COMMENT '中文译文（段落与 content 一一对应）',
  publish_date  DATE         DEFAULT NULL            COMMENT '原文发布日期',
  create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_category (category),
  KEY idx_difficulty (difficulty)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阅读文章';

-- ---------------------------------------------------------------------
-- 2. 阅读记录
--    UNIQUE(user_id, article_id) + ON DUPLICATE KEY UPDATE 实现 upsert：
--    · duration_sec 累加（同一篇反复读，时长累计）
--    · progress 取较大值（进度只进不退）
--    · read_count 每打开一次 +1
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_reading_record;
CREATE TABLE t_reading_record (
  id            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id       BIGINT   NOT NULL                COMMENT '用户ID',
  article_id    BIGINT   NOT NULL                COMMENT '文章ID',
  progress      INT      DEFAULT 0               COMMENT '阅读进度百分比 0-100',
  duration_sec  INT      DEFAULT 0               COMMENT '累计阅读时长（秒）',
  finished      TINYINT  DEFAULT 0               COMMENT '0在读 1已读完',
  read_count    INT      DEFAULT 1               COMMENT '打开次数',
  first_time    DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '首次阅读时间',
  last_time     DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近阅读时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_article (user_id, article_id),
  KEY idx_user_finish (user_id, finished)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='阅读记录（进度 + 时长）';
