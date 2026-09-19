-- ============================================================
-- 外部数据源配置表（设置页可改）
-- ============================================================
-- 设计说明：
--   1. 全局配置（不区分用户），key-value 结构，新增配置项无需改表结构。
--   2. 只存「覆盖值」，未写入的 key 由后端 SettingsServiceImpl 用默认值兜底，
--      因此本表初始可以为空 —— 下面的 INSERT 只是把默认值显式化，便于人工查看。
--   3. baseUrl 留空表示「用代码内置的默认地址」，填了则覆盖（这就是「换 API 源」）。
-- ============================================================

USE cet6_sprint;

CREATE TABLE IF NOT EXISTS t_app_config (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    cfg_key     VARCHAR(64)  NOT NULL COMMENT '配置键',
    cfg_value   VARCHAR(512) NOT NULL DEFAULT '' COMMENT '配置值（空串表示使用默认）',
    remark      VARCHAR(128)          DEFAULT NULL COMMENT '说明，便于运维',
    update_time DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cfg_key (cfg_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT ='应用配置（key-value）';

-- ------------------------------------------------------------
-- 默认值
--   dict      : datamuse | dictionaryapi | off
--   pron      : youdao   | dictionaryapi | off
--   translate : mymemory | off
-- 实测（2026-09-14）：
--   datamuse       稳定 ~0.8s，免密钥，返回词性 + ARPAbet 音标 + 英文释义
--   youdao dictvoice 稳定 ~0.38s，免密钥，直接返回 mp3
--   mymemory       稳定 ~1.3s，免密钥，但单次 q ≤ 500 字符
--   dictionaryapi  当时的源站故障（未缓存词返回 522），保留为可选项
-- ------------------------------------------------------------
INSERT INTO t_app_config (cfg_key, cfg_value, remark) VALUES
    ('ext.enabled',          '1',        '外部数据源总开关 1开/0关'),
    ('ext.dict.source',      'datamuse', '词典源：datamuse/dictionaryapi/off'),
    ('ext.dict.baseUrl',     '',         '词典源自定义地址，空=用默认'),
    ('ext.pron.source',      'youdao',   '发音源：youdao/dictionaryapi/off'),
    ('ext.pron.baseUrl',     '',         '发音源自定义地址，空=用默认'),
    ('ext.translate.source', 'mymemory', '翻译源：mymemory/off'),
    ('ext.translate.baseUrl','',         '翻译源自定义地址，空=用默认'),
    ('ext.cacheMinutes',     '10',       '外部结果缓存分钟数'),
    ('ext.timeoutMs',        '4000',     '单次外部请求超时（毫秒）。配合 60 秒失败熔断，避免源站挂掉时拖慢查词')
ON DUPLICATE KEY UPDATE cfg_key = cfg_key;
