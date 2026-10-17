-- 应用启动时自动建表（H2 内存库）。切换 MySQL 时，把下面这些语句在 MySQL 里执行一遍即可。
-- 注意：表名用 users 而不是 user，因为 user 在 H2/MySQL 里是保留关键字，直接用会报语法错误。
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(100) NOT NULL,
    email       VARCHAR(100),
    role        VARCHAR(20)  DEFAULT 'USER',
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- 简历表：一条记录 = 一份简历（某个用户上传/粘贴的）
-- 设计要点（面试可讲）：
--   1) user_id 关联 users 表，表示"这份简历是谁的"。一个用户可以有多份简历
--      （比如投 Java 岗一份、投前端岗一份），所以是 多对一 关系。
--   2) raw_text 存用户给我们的原始文本（不做任何加工，可回溯）。
--   3) parsed_json 存大模型返回的结构化 JSON（字符串形式）。
--      为什么不把姓名/学校拆成一列一列？因为简历字段经常变、还可能是数组（多段实习、
--      多个项目）。用 JSON 存，扩展性远好于一板一眼建列，改字段不用改表结构。
--      MySQL 里对应用 JSON 或 TEXT 列，H2 里用 CLOB。
--   4) status 记录解析状态，方便前端展示"解析中/解析成功/解析失败"。
CREATE TABLE IF NOT EXISTS resumes (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    file_name    VARCHAR(255),
    raw_text     CLOB         NOT NULL,
    parsed_json  CLOB,
    status       VARCHAR(20)  DEFAULT 'PARSING',
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

-- 按用户查简历是最高频的查询（"我的简历列表"），加索引避免全表扫描
CREATE INDEX IF NOT EXISTS idx_resumes_user_id ON resumes(user_id);

-- 岗位表：一条记录 = 一份岗位 JD（用户自己粘贴进来的）
-- 设计要点（面试可讲）：
--   1) 为什么是"用户粘贴 JD"而不是"我们爬招聘网站"？
--      爬虫涉及合规风险（违反网站服务条款、被反爬、数据版权）。MVP 阶段让用户自己粘贴，
--      既绕开合规问题，又能拿到"用户真正想投"的岗位 —— 对匹配准确度反而更好。
--      这是很典型的"先做能合法落地的最小方案"的工程取舍。
--   2) raw_text 存 JD 原文（可回溯）；parsed_json 存 AI 打的标签
--      （岗位名/公司/城市/薪资/必备技能/加分技能/注意事项…）。
--      和简历表一样的思路：结构不固定的内容用 JSON 存，扩展不用改表。
--   3) raw_text_hash 存 JD 原文的 SHA-256，专门用来【去重】。
--      为什么不直接拿 raw_text 做比较？因为 JD 原文是长文本（CLOB/TEXT），
--      用大字段做等值比较既慢又容易踩数据库差异（CLOB 比较、字符集、换行符差异）。
--      先算一个定长哈希（64 位十六进制），用定长字符串比较，快且稳。
--      这个套路在"文件去重""消息幂等"里到处都是，面试常问。
CREATE TABLE IF NOT EXISTS jobs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    raw_text        CLOB         NOT NULL,
    raw_text_hash   VARCHAR(64)  NOT NULL,
    parsed_json     CLOB,
    status          VARCHAR(20)  DEFAULT 'PARSING',
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_jobs_user_id ON jobs(user_id);

-- 去重查询是"按用户 + 按哈希"，所以建联合索引（复合索引最左前缀原则：user_id 在前）
CREATE INDEX IF NOT EXISTS idx_jobs_user_hash ON jobs(user_id, raw_text_hash);

-- 匹配结果表：一条记录 = 一次「简历×岗位」的 AI 评估
-- 设计要点（面试可讲）：
--   1) 同时关联 user_id / resume_id / job_id：每次评估都归当前用户所有，且能从任一端反查。
--      不存外键约束（H2 默认行为也不同），靠 Service 层保证不会被别人读到别人的数据。
--   2) score 单独一列：方便"按分数排序""看历史最高分"这类查询，不必每次解 JSON。
--      parsed_json 里再放 AI 输出的详细理由/亮点/差距分析。
--   3) 错误信息单独一列（error_message），而不是塞进 JSON：方便前端统一展示"AI 失败原因"。
CREATE TABLE IF NOT EXISTS match_results (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    resume_id     BIGINT       NOT NULL,
    job_id        BIGINT       NOT NULL,
    score         INT,
    parsed_json   CLOB,
    status        VARCHAR(20)  DEFAULT 'PARSING',
    error_message VARCHAR(500),
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_match_user ON match_results(user_id);
CREATE INDEX IF NOT EXISTS idx_match_user_resume ON match_results(user_id, resume_id);
CREATE INDEX IF NOT EXISTS idx_match_user_job ON match_results(user_id, job_id);

-- 模拟面试会话表：一次会话 = 针对某个岗位的多题问答
-- 设计要点（面试可讲）：
--   1) questions_json 缓存 AI 一开始出的所有题目（数组形式）：题目一旦生成不再变，
--      避免每次访问会话都重新调一次大模型（既费钱又会出现"题目一会一会不一样"的体验问题）。
--   2) status: ACTIVE（进行中）/ FINISHED（已答完，已生成总结）。
--   3) summary_json：所有题答完之后，由 AI 一次性给的总评。
--   4) job_title 冗余存一份：岗位可能被删，但会话的快照里仍能看到当时面的是什么岗。
CREATE TABLE IF NOT EXISTS interview_sessions (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT       NOT NULL,
    job_id         BIGINT       NOT NULL,
    job_title      VARCHAR(200),
    questions_json CLOB,
    status         VARCHAR(20)  DEFAULT 'ACTIVE',
    total_score    INT          DEFAULT 0,
    summary_json   CLOB,
    created_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_iv_user ON interview_sessions(user_id);

-- 模拟面试答题表：一条记录 = 一道题 + 用户的回答 + AI 的反馈
-- 设计要点：
--   1) 题目文本快照在本表（question_text）：题目在 sessions 表是 JSON 整体存的，
--      答题时按数组下标定位到具体题目 → 不依赖解析 JSON 也能拿到题目文本。
--   2) score 0-100 单题分；feedback 单题点评。
--   3) 用 (session_id, question_index) 当"一道题只能答一次"的天然唯一键（不强制建唯一索引，
--      因为允许"覆盖式重答"——后面会做）。
CREATE TABLE IF NOT EXISTS interview_answers (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id     BIGINT       NOT NULL,
    question_index INT          NOT NULL,
    question_text  VARCHAR(1000) NOT NULL,
    user_answer    CLOB,
    score          INT,
    feedback       CLOB,
    created_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_iv_ans_session ON interview_answers(session_id);

-- 订单表：一次"模拟支付"= 一条记录
-- 设计要点（面试可讲）：
--   1) order_no 是对用户暴露的业务订单号（区别于自增 id）：自增 id 是数据库内部连续数字，
--      不适合做"对外凭据"（容易被枚举、爬取、撞库）。
--      这里用 UUID 风格的字符串，前端拿去做支付时引用的是它。
--   2) amount_cents 用"分"为单位：金额永远不要用浮点数（0.1+0.2=0.30000000000000004 的梗），
--      用整数"分"是最稳妥的工程惯例。
--   3) status: PENDING（已下单未支付）/ PAID（已支付）/ REFUNDED（已退款）。
CREATE TABLE IF NOT EXISTS orders (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    order_no      VARCHAR(64)  NOT NULL UNIQUE,
    plan          VARCHAR(20)  NOT NULL,
    plan_name     VARCHAR(50)  NOT NULL,
    amount_cents  INT          NOT NULL,
    status        VARCHAR(20)  DEFAULT 'PENDING',
    paid_at       TIMESTAMP,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_orders_user ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_user_status ON orders(user_id, status);

-- 会员表：每个用户最多一条有效会员（通过 expires_at 判定是否还有效）
-- 设计要点（面试可讲）：
--   1) 为什么用"一行存一份当前会员"而不是"历史全部记录"？
--      MVP 阶段简化；当前状态从最新一行查就行。生产环境如果要按月对账、再加一张 history 表。
--   2) expires_at 是判断会员是否有效的唯一权威字段（user_id 是 UNIQUE），
--      所以即使某条记录"过期了"，也只是 expires_at < now() 而不会被删 —— 删了反而麻烦。
--   3) auto_renew: 自动续费标志。MVP 不接真实网关，所以这个字段暂时只是占位。
CREATE TABLE IF NOT EXISTS memberships (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL UNIQUE,
    plan        VARCHAR(20)  NOT NULL,
    starts_at   TIMESTAMP    NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    auto_renew  BOOLEAN      DEFAULT FALSE,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX IF NOT EXISTS idx_memberships_user ON memberships(user_id);
