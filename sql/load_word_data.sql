-- =====================================================================
-- 从 ECDICT 清洗后的数据导入词库
-- 前置：已执行 etl.py 生成 word_import.tsv
-- 执行：mysql -uroot -p****** --local-infile=1 cet6_sprint < load_word_data.sql
-- 注意：客户端必须带 --local-infile=1，服务端需开启 local_infile
-- =====================================================================

SET GLOBAL local_infile = 1;

USE cet6_sprint;

TRUNCATE TABLE t_word;

LOAD DATA LOCAL INFILE 'D:/WorkBuddyFiles/task/cet6-build/word_import.tsv'
INTO TABLE t_word
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t' ESCAPED BY '\\'
LINES TERMINATED BY '\n'
(word, phonetic, pos, definition, translation, tag, collins, oxford, bnc, frq, exchange);

-- 回填六级标记：tag 是空格分隔的多标签串，先转成逗号再用 FIND_IN_SET 精确匹配
UPDATE t_word SET is_cet6 = 1 WHERE FIND_IN_SET('cet6', REPLACE(tag, ' ', ','));

SELECT COUNT(*) AS total_words FROM t_word;
SELECT COUNT(*) AS cet6_words FROM t_word WHERE is_cet6 = 1;
SELECT COUNT(*) AS cet6_words FROM t_word WHERE tag LIKE '%cet6%';
