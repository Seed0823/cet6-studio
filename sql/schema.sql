-- =====================================================================
-- CET-6 六级冲刺学习平台 · 数据库结构
-- MySQL 8.0+ / utf8mb4
-- 说明：词库数据（t_word）由 ECDICT 开源词典离线导入，不在此脚本内
-- =====================================================================

CREATE DATABASE IF NOT EXISTS cet6_sprint
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_general_ci;

USE cet6_sprint;

-- ---------------------------------------------------------------------
-- 1. 用户（单账号体系）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_user;
CREATE TABLE t_user (
  id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  username      VARCHAR(50)  NOT NULL                COMMENT '登录账号',
  password      VARCHAR(100) NOT NULL                COMMENT '密码（BCrypt 加密，不可逆）',
  nickname      VARCHAR(50)  DEFAULT NULL            COMMENT '昵称',
  exam_date     DATE         DEFAULT NULL            COMMENT 'CET-6 考试日期',
  target_score  INT          DEFAULT 425             COMMENT '目标分数',
  create_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- ---------------------------------------------------------------------
-- 2. 词库（ECDICT 导入，同时服务「背单词」和「查词」两个场景）
--    区分方式：tag 字段含 'cet6' 的即为六级大纲词
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_word;
CREATE TABLE t_word (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  word         VARCHAR(100) NOT NULL                COMMENT '单词',
  phonetic     VARCHAR(100) DEFAULT NULL            COMMENT '音标',
  pos          VARCHAR(50)  DEFAULT NULL            COMMENT '词性',
  definition   TEXT                                 COMMENT '英文释义',
  translation  TEXT                                 COMMENT '中文释义',
  tag          VARCHAR(64)  DEFAULT NULL            COMMENT '考试标签：zk/gk/cet4/cet6/ky/toefl/ielts/gre',
  collins      TINYINT      DEFAULT 0               COMMENT '柯林斯星级 0-5',
  oxford       TINYINT      DEFAULT 0               COMMENT '是否牛津三千核心词',
  bnc          INT          DEFAULT 0               COMMENT 'BNC 词频序（越小越高频）',
  frq          INT          DEFAULT 0               COMMENT '当代语料库词频序',
  exchange     VARCHAR(255) DEFAULT NULL            COMMENT '词形变化',
  is_cet6      TINYINT      DEFAULT 0               COMMENT '是否六级大纲词（冗余标记，避免 LIKE 全表扫描）',
  PRIMARY KEY (id),
  UNIQUE KEY uk_word (word),
  KEY idx_tag (tag),
  KEY idx_cet6 (is_cet6),
  KEY idx_frq (frq)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='词库（ECDICT）';

-- ---------------------------------------------------------------------
-- 3. 查词记录 —— 需求：记录每个词查了多少次，按次数倒序
--    核心：UNIQUE(user_id, word) + ON DUPLICATE KEY UPDATE 实现 upsert 累加
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_dict_query;
CREATE TABLE t_dict_query (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id      BIGINT       NOT NULL                COMMENT '用户ID',
  word         VARCHAR(100) NOT NULL                COMMENT '被查询的单词（原形）',
  query_count  INT          DEFAULT 1               COMMENT '累计查询次数',
  first_time   DATETIME     DEFAULT CURRENT_TIMESTAMP COMMENT '首次查询时间',
  last_time    DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '最近查询时间',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_word (user_id, word),
  KEY idx_rank (user_id, query_count DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='查词记录（热词榜数据源）';

-- ---------------------------------------------------------------------
-- 4. 单词掌握度（背单词：会 / 模糊 / 不认识）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_word_grade;
CREATE TABLE t_word_grade (
  id            BIGINT   NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id       BIGINT   NOT NULL                COMMENT '用户ID',
  word_id       BIGINT   NOT NULL                COMMENT '词库ID',
  grade         TINYINT  DEFAULT 0               COMMENT '0不认识 1模糊 2认识',
  review_count  INT      DEFAULT 0               COMMENT '复习次数',
  next_review   DATE     DEFAULT NULL            COMMENT '艾宾浩斯下次复习日期',
  update_time   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_word (user_id, word_id),
  KEY idx_next_review (user_id, next_review)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='单词掌握度';

-- ---------------------------------------------------------------------
-- 5. 错词本
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_wrong_word;
CREATE TABLE t_wrong_word (
  id           BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id      BIGINT      NOT NULL                COMMENT '用户ID',
  word_id      BIGINT      NOT NULL                COMMENT '词库ID',
  source       VARCHAR(20) DEFAULT 'quiz'          COMMENT '来源：quiz/exam/dict',
  wrong_count  INT         DEFAULT 1               COMMENT '答错次数',
  mastered     TINYINT     DEFAULT 0               COMMENT '0未攻克 1已攻克',
  create_time  DATETIME    DEFAULT CURRENT_TIMESTAMP,
  update_time  DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_word (user_id, word_id),
  KEY idx_mastered (user_id, mastered)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='错词本';

-- ---------------------------------------------------------------------
-- 6. 测验记录（每次自测一条）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_quiz_record;
CREATE TABLE t_quiz_record (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id      BIGINT       NOT NULL                COMMENT '用户ID',
  total        INT          DEFAULT 0               COMMENT '题目总数',
  correct      INT          DEFAULT 0               COMMENT '答对题数',
  accuracy     DECIMAL(5,2) DEFAULT 0               COMMENT '正确率 %',
  cost_second  INT          DEFAULT 0               COMMENT '耗时（秒）',
  create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_user_time (user_id, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测验记录';

-- 6.1 测验明细（每题作答，用于错题归因）
DROP TABLE IF EXISTS t_quiz_detail;
CREATE TABLE t_quiz_detail (
  id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  record_id      BIGINT       NOT NULL                COMMENT '所属测验记录',
  word_id        BIGINT       DEFAULT NULL            COMMENT '词库ID',
  question       VARCHAR(500) DEFAULT NULL            COMMENT '题干',
  options_json   VARCHAR(800) DEFAULT NULL            COMMENT '选项 JSON',
  correct_index  TINYINT      DEFAULT NULL            COMMENT '正确选项下标',
  chosen_index   TINYINT      DEFAULT NULL            COMMENT '用户选择下标',
  is_right       TINYINT      DEFAULT 0               COMMENT '是否答对',
  PRIMARY KEY (id),
  KEY idx_record (record_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='测验明细';

-- ---------------------------------------------------------------------
-- 7. 学习日志（每日各模块学习时长，统计页数据源）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_study_log;
CREATE TABLE t_study_log (
  id            BIGINT      NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id       BIGINT      NOT NULL                COMMENT '用户ID',
  study_date    DATE        NOT NULL                COMMENT '学习日期',
  module        VARCHAR(20) NOT NULL                COMMENT '模块：word/quiz/dict',
  duration_min  INT         DEFAULT 0               COMMENT '学习时长（分钟）',
  word_count    INT         DEFAULT 0               COMMENT '学习词数',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_date_module (user_id, study_date, module),
  KEY idx_date (user_id, study_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学习日志';

-- ---------------------------------------------------------------------
-- 8. 打卡（连续天数计算）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_checkin;
CREATE TABLE t_checkin (
  id            BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id       BIGINT NOT NULL                COMMENT '用户ID',
  checkin_date  DATE   NOT NULL                COMMENT '打卡日期',
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_date (user_id, checkin_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='打卡记录';

-- ---------------------------------------------------------------------
-- 9. 每日任务（今日学习页的 4 项任务）
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_task_record;
CREATE TABLE t_task_record (
  id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  user_id     BIGINT       NOT NULL                COMMENT '用户ID',
  task_date   DATE         NOT NULL                COMMENT '所属日期',
  task_key    VARCHAR(40)  NOT NULL                COMMENT '任务标识：word/review/quiz/dict',
  task_name   VARCHAR(100) DEFAULT NULL            COMMENT '任务名称',
  target      INT          DEFAULT 0               COMMENT '目标数量',
  finished    INT          DEFAULT 0               COMMENT '已完成数量',
  status      TINYINT      DEFAULT 0               COMMENT '0未完成 1已完成',
  update_time DATETIME     DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_user_date_key (user_id, task_date, task_key)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='每日任务';
