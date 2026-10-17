-- =================================================================
-- MySQL 版建表语句（项目启动时自动执行）
-- 说明：
--   1) MySQL 8.0+ 支持 CREATE TABLE / CREATE INDEX 的 IF NOT EXISTS，
--      重复启动不会清数据，只是 no-op；
--   2) 默认 ENGINE=InnoDB + utf8mb4（emoji/中文都支持），H2 不需要；
--   3) TEXT/LONGTEXT 替代 H2 的 CLOB（应用代码用 String 读写，没差异）；
--   4) BOOLEAN MySQL 8 内部映射为 TINYINT(1)，应用代码用 boolean 没差异。
--   5) 表名 users 是必须的：user 是 MySQL/H2 双保留字，会报语法错误。
-- =================================================================

-- ====== 用户表 ======
CREATE TABLE IF NOT EXISTS users (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    username    VARCHAR(50)  NOT NULL UNIQUE,
    password    VARCHAR(100) NOT NULL,
    email       VARCHAR(100),
    role        VARCHAR(20)  DEFAULT 'USER',
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ====== 简历表 ======
CREATE TABLE IF NOT EXISTS resumes (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id      BIGINT       NOT NULL,
    file_name    VARCHAR(255),
    raw_text     LONGTEXT     NOT NULL,
    parsed_json  LONGTEXT,
    status       VARCHAR(20)  DEFAULT 'PARSING',
    created_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_resumes_user_id ON resumes(user_id);

-- ====== 岗位表 ======
CREATE TABLE IF NOT EXISTS jobs (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    raw_text        LONGTEXT     NOT NULL,
    raw_text_hash   VARCHAR(64)  NOT NULL,
    parsed_json     LONGTEXT,
    status          VARCHAR(20)  DEFAULT 'PARSING',
    created_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_jobs_user_id ON jobs(user_id);
CREATE INDEX IF NOT EXISTS idx_jobs_user_hash ON jobs(user_id, raw_text_hash);

-- ====== 匹配结果表 ======
CREATE TABLE IF NOT EXISTS match_results (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id       BIGINT       NOT NULL,
    resume_id     BIGINT       NOT NULL,
    job_id        BIGINT       NOT NULL,
    score         INT,
    parsed_json   LONGTEXT,
    status        VARCHAR(20)  DEFAULT 'PARSING',
    error_message VARCHAR(500),
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_match_user ON match_results(user_id);
CREATE INDEX IF NOT EXISTS idx_match_user_resume ON match_results(user_id, resume_id);
CREATE INDEX IF NOT EXISTS idx_match_user_job ON match_results(user_id, job_id);

-- ====== 面试会话表 ======
CREATE TABLE IF NOT EXISTS interview_sessions (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id        BIGINT       NOT NULL,
    job_id         BIGINT       NOT NULL,
    job_title      VARCHAR(200),
    questions_json LONGTEXT,
    status         VARCHAR(20)  DEFAULT 'ACTIVE',
    total_score    INT          DEFAULT 0,
    summary_json   LONGTEXT,
    created_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_iv_user ON interview_sessions(user_id);

-- ====== 面试答题表 ======
CREATE TABLE IF NOT EXISTS interview_answers (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id     BIGINT        NOT NULL,
    question_index INT           NOT NULL,
    question_text  VARCHAR(1000) NOT NULL,
    user_answer    LONGTEXT,
    score          INT,
    feedback       LONGTEXT,
    created_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP     DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_iv_ans_session ON interview_answers(session_id);

-- ====== 订单表 ======
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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_orders_user ON orders(user_id);
CREATE INDEX IF NOT EXISTS idx_orders_user_status ON orders(user_id, status);

-- ====== 会员表 ======
CREATE TABLE IF NOT EXISTS memberships (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT       NOT NULL UNIQUE,
    plan        VARCHAR(20)  NOT NULL,
    starts_at   TIMESTAMP    NOT NULL,
    expires_at  TIMESTAMP    NOT NULL,
    auto_renew  TINYINT(1)   DEFAULT 0,
    created_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP    DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE INDEX IF NOT EXISTS idx_memberships_user ON memberships(user_id);
