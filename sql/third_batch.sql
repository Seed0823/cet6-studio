-- =====================================================================
-- CET6 Studio · 第三批模块 · 建表 + 种子数据
-- 听力(listening) / 写作(writing) / 翻译(translation)
-- MySQL 8.0+ / utf8mb8
-- 执行：mysql -uroot -proot -e "source D:/WorkBuddyFiles/outputs/cet6-studio/sql/third_batch.sql"
-- =====================================================================

USE cet6_sprint;

-- ---------------------------------------------------------------------
-- 1. 翻译练习（中译英）
--    chinese_text 待译中文；reference_en 参考译文；key_points 用于自测覆盖率的关键英文词/短语
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_translation;
CREATE TABLE t_translation (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  category     VARCHAR(20)  NOT NULL                COMMENT '分类：culture文化 / society社会 / tech科技 / economy经济',
  title        VARCHAR(200) NOT NULL                COMMENT '主题标题',
  chinese_text TEXT                                 COMMENT '待翻译中文',
  reference_en TEXT                                 COMMENT '参考英文译文',
  key_points   VARCHAR(500) DEFAULT NULL            COMMENT '关键英文词/短语，分号分隔，用于自测覆盖率',
  difficulty   TINYINT      DEFAULT 3               COMMENT '难度 1-5',
  create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='翻译练习';

-- ---------------------------------------------------------------------
-- 2. 写作练习（命题作文）
--    prompt 命题；requirement 字数要求描述；outline_points 提纲要点(\n 分隔)；
--    reference_essay 参考范文；keywords 用于自测覆盖率的关键词
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_writing;
CREATE TABLE t_writing (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  category        VARCHAR(20)  NOT NULL                COMMENT '分类：argument议论文 / letter书信 / chart图表',
  prompt          TEXT                                 COMMENT '作文命题（中文）',
  requirement     VARCHAR(300) DEFAULT NULL            COMMENT '字数/体裁要求描述',
  outline_points  TEXT                                 COMMENT '提纲要点，换行分隔',
  reference_essay TEXT                                 COMMENT '参考范文',
  keywords        VARCHAR(500) DEFAULT NULL            COMMENT '关键英文词/短语，分号分隔，用于自测覆盖率',
  min_words       INT          DEFAULT 150            COMMENT '最少词数',
  max_words       INT          DEFAULT 200            COMMENT '最多词数',
  create_time     DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='写作练习';

-- ---------------------------------------------------------------------
-- 3. 听力练习（原文 + 选择题）
--    script 听力原文（同时用于浏览器语音合成朗读）；category 题材
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS t_listening;
CREATE TABLE t_listening (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  title        VARCHAR(200) NOT NULL                COMMENT '标题',
  category     VARCHAR(20)  NOT NULL                COMMENT '分类：dialog对话 / passage短文 / news新闻',
  script       TEXT                                 COMMENT '听力原文（用于朗读与对照）',
  difficulty   TINYINT      DEFAULT 3               COMMENT '难度 1-5',
  create_time  DATETIME     DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_category (category)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='听力练习';

-- 3.1 听力选择题
DROP TABLE IF EXISTS t_listening_question;
CREATE TABLE t_listening_question (
  id           BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  listening_id BIGINT       NOT NULL                COMMENT '所属听力',
  question     VARCHAR(400) NOT NULL                COMMENT '题干',
  options_json VARCHAR(800) NOT NULL                COMMENT '选项 JSON 数组',
  answer_index TINYINT      NOT NULL                COMMENT '正确选项下标',
  `explain`   VARCHAR(400) DEFAULT NULL            COMMENT '解析',
  PRIMARY KEY (id),
  KEY idx_listening (listening_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='听力选择题';

-- =====================================================================
-- 种子数据
-- =====================================================================

-- 翻译：6 篇（覆盖文化/社会/科技/经济）
INSERT INTO t_translation (category, title, chinese_text, reference_en, key_points, difficulty) VALUES
('culture', '中国茶文化',
 '中国茶文化源远流长。茶不仅是一种饮品，更是一种生活态度，体现了中国人对自然与平和的追求。',
 'Chinese tea culture has a long history. Tea is not only a beverage but also a way of life, showing how Chinese people value nature and inner peace.',
 'tea culture;beverage;way of life;value nature;inner peace', 3),
('culture', '春节',
 '春节是中国最重要的传统节日。无论多远，人们都会回家与家人团聚，共度佳节。',
 'The Spring Festival is the most important traditional holiday in China. No matter how far away they are, people return home to reunite with families and celebrate together.',
 'Spring Festival;traditional holiday;reunite with families;celebrate together', 2),
('tech', '中国高铁',
 '中国高铁网络是世界上最庞大、最高效的高铁系统之一，大大缩短了城市间的通行时间。',
 'China high-speed rail network is among the largest and most efficient systems in the world, greatly reducing travel time between cities.',
 'high-speed rail;largest and most efficient;reducing travel time;between cities', 4),
('society', '移动支付',
 '如今，移动支付在中国极为普及，人们几乎可以用手机完成所有日常消费和转账。',
 'Today mobile payment is extremely popular in China. People can finish almost all daily shopping and money transfers with only a phone.',
 'mobile payment;extremely popular;daily shopping;money transfers;phone', 3),
('culture', '太极拳',
 '太极拳是一种中国传统健身运动，动作缓慢柔和，有助于放松身心、增强体质。',
 'Tai Chi is a traditional Chinese exercise. Its slow and gentle movements help relax body and mind and improve physical fitness.',
 'Tai Chi;traditional Chinese exercise;slow and gentle;relax body and mind;physical fitness', 3),
('economy', '丝绸之路',
 '古代丝绸之路连接了亚洲与欧洲，促进了沿线各国的贸易往来与文化交流。',
 'The ancient Silk Road connected Asia and Europe and promoted trade and cultural exchange among countries along the route.',
 'Silk Road;connected Asia and Europe;promoted trade;cultural exchange;along the route', 4);

-- 写作：6 篇（议论文为主）
INSERT INTO t_writing (category, prompt, requirement, outline_points, reference_essay, keywords, min_words, max_words) VALUES
('argument', 'For this part, you are allowed 30 minutes to write a short essay on "The Impact of Online Learning". You should write at least 150 words but no more than 200 words.',
 '写一篇 150-200 词的议论文',
 '1. 在线学习的兴起与普及\n2. 其优势：便捷、资源丰富\n3. 其不足：缺乏互动、需自律\n4. 你的看法',
 'Online learning has grown quickly in recent years and become a common choice for many students. Its main advantage is convenience. Learners can study anytime and anywhere, and they have access to abundant resources on the internet. However, online learning also has weaknesses. It lacks face-to-face interaction, and it requires strong self-discipline. Without a teacher nearby, some students lose focus. In my view, online learning is a useful tool, but it should complement rather than replace traditional classrooms.',
 'online learning;convenience;abundant resources;face-to-face interaction;self-discipline', 150, 200),
('argument', 'For this part, you are allowed 30 minutes to write a short essay on "Part-time Jobs for College Students". You should write at least 150 words but no more than 200 words.',
 '写一篇 150-200 词的议论文',
 '1. 大学生兼职的现象\n2. 利：赚零花、积累经验\n3. 弊：占用精力，需平衡\n4. 你的建议',
 'Many college students take part-time jobs during their free time. On the one hand, part-time work helps students earn pocket money and gain real social experience. They learn to communicate and to manage time. On the other hand, a job may take energy away from study if not controlled well. I believe students should choose simple and flexible jobs and keep study as the first priority. A good balance brings both income and growth.',
 'part-time jobs;pocket money;social experience;manage time;first priority', 150, 200),
('argument', 'For this part, you are allowed 30 minutes to write a short essay on "Protecting the Environment". You should write at least 150 words but no more than 200 words.',
 '写一篇 150-200 词的议论文',
 '1. 环保是每个人的责任\n2. 日常小事也能见效\n3. 众人拾柴火焰高\n4. 青年应带头',
 'Environmental protection has become a shared duty for everyone. Small actions at home can make a big difference. For example, we can save water, sort waste, and use public transport instead of driving. Many people think one person can do little, but if millions act together, the result is huge. In short, protecting the Earth starts with daily habits, and young people should lead by example.',
 'environmental protection;shared duty;save water;sort waste;public transport', 150, 200),
('argument', 'For this part, you are allowed 30 minutes to write a short essay on "The Influence of Artificial Intelligence". You should write at least 150 words but no more than 200 words.',
 '写一篇 150-200 词的议论文',
 '1. 人工智能改变生活\n2. 其应用举例\n3. 人们的担忧\n4. 你的态度',
 'Artificial intelligence is changing our lives in many ways. It helps doctors find disease, assists drivers with safety, and makes phones smarter. At the same time, some worry that AI may replace human jobs. I think AI is a tool that increases efficiency, not a full replacement for human thought. We should learn to use it well while keeping our own creativity and judgment.',
 'artificial intelligence;changing lives;increase efficiency;replace human jobs;creativity', 150, 200),
('argument', 'For this part, you are allowed 30 minutes to write a short essay on "The Value of Honesty". You should write at least 150 words but no more than 200 words.',
 '写一篇 150-200 词的议论文',
 '1. 诚实是基本美德\n2. 诚实赢得信任\n3. 谎言的代价\n4. 你的看法',
 'Honesty is a basic virtue in human society. An honest person wins trust from others and feels peaceful inside. In contrast, lying may bring short gain but long loss. Schools and families should teach children to be honest from an early age. In my opinion, no matter how the world changes, honesty remains the foundation of real success.',
 'honesty;basic virtue;win trust;short gain;foundation of success', 150, 200),
('argument', 'For this part, you are allowed 30 minutes to write a short essay on "Short Videos and Young People". You should write at least 150 words but no more than 200 words.',
 '写一篇 150-200 词的议论文',
 '1. 短视频在年轻人中流行\n2. 利：娱乐、快资讯\n3. 弊：占用时间、影响作息\n4. 关键是自律',
 'Short videos are now very popular among young people. They offer fun and quick information in spare time. Yet spending too much time on them can hurt study and sleep. The key is self-control. We should enjoy short videos as relaxation, not let them control our days. Used wisely, they can also teach useful skills.',
 'short videos;popular among young;quick information;self-control;useful skills', 150, 200);

-- 听力：4 篇（对话/短文/新闻），每篇 2-3 题
INSERT INTO t_listening (title, category, script, difficulty) VALUES
('跑步俱乐部', 'dialog',
 'W: Hello, I heard you joined the school running club last month. M: Yes, I run three times a week now. At first I felt very tired, but now I feel much healthier. W: That is great. Do you join the race next month? M: I plan to. It is a five kilometer fun run.', 2),
('图书馆须知', 'passage',
 'Library is a quiet place for study. Our school library opens at eight in the morning and closes at ten at night. Students can borrow up to five books for one month. There is also a reading room on the second floor with newspapers from many countries. Last week, the library added a new area for group discussion.', 2),
('市中心新公园', 'news',
 'A new park opened in the city center yesterday. It covers about twenty thousand square meters. The park has a small lake, many trees, and a walking path around it. The mayor said the park was built to give citizens more space for rest and exercise. More than two thousand people visited the park on the first day.', 3),
('借书与打印', 'dialog',
 'M: Excuse me, where can I find the history books? W: They are on the third floor, section H. M: Thank you. And where is the print service? W: The print room is on the first floor, next to the door. M: Great, I also need to return this book. W: You can leave it at the desk here.', 2);

-- 听力题目（listening_id 按插入顺序 1..4）
INSERT INTO t_listening_question (listening_id, question, options_json, answer_index, `explain`) VALUES
(1, 'What club did the man join?', '["A dancing club","A running club","A reading club","A music club"]', 1, '原文提到 joined the school running club。'),
(1, 'How often does the man run?', '["Once a week","Twice a week","Three times a week","Every day"]', 2, '原文 I run three times a week。'),
(1, 'What event will he join next month?', '["A ten kilometer race","A five kilometer fun run","A swimming match","A bike trip"]', 1, '原文 It is a five kilometer fun run。'),
(2, 'When does the library close?', '["Six in the evening","Eight in the evening","Ten at night","Nine in the morning"]', 2, '原文 closes at ten at night。'),
(2, 'How many books can a student borrow?', '["Three","Five","Ten","Two"]', 1, '原文 borrow up to five books。'),
(2, 'Where is the reading room?', '["On the first floor","On the second floor","On the ground floor","Near the door"]', 1, '原文 reading room on the second floor。'),
(3, 'When did the new park open?', '["Yesterday","Last week","Next month","Tomorrow"]', 0, '原文 opened yesterday。'),
(3, 'What is around the walking path?', '["A small lake","A shopping mall","A school","A bus station"]', 0, '原文 a small lake ... and a walking path around it。'),
(3, 'Why was the park built?', '["To sell tickets","To give space for rest and exercise","To hold a market","To park cars"]', 1, '原文 built to give citizens more space for rest and exercise。'),
(4, 'Where are the history books?', '["First floor","Third floor section H","Second floor","Near the door"]', 1, '原文 on the third floor, section H。'),
(4, 'Where is the print room?', '["Third floor","First floor next to the door","Reading room","Outside"]', 1, '原文 print room is on the first floor, next to the door。');
