-- ╔═══════════════════════════════════════════════════════════════════╗
-- ║  Code Review Bot — PostgreSQL Schema                            ║
-- ║  Version: 1.0.0                                                 ║
-- ╚═══════════════════════════════════════════════════════════════════╝

-- ─── Tenants ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS tenants (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255)    NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    github_org_or_user VARCHAR(100),
    plan            VARCHAR(10)     NOT NULL DEFAULT 'FREE',
    role            VARCHAR(20)     NOT NULL DEFAULT 'REPO_OWNER',
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_tenants_plan CHECK (plan IN ('FREE', 'PRO')),
    CONSTRAINT chk_tenants_role CHECK (role IN ('ADMIN', 'REPO_OWNER', 'VIEWER'))
);

CREATE INDEX IF NOT EXISTS idx_tenants_email ON tenants (email);
CREATE INDEX IF NOT EXISTS idx_tenants_github_org ON tenants (github_org_or_user);

-- ─── Repos ──────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS repos (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID            NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    repo_full_name          VARCHAR(255)    NOT NULL,
    github_installation_id  BIGINT          NOT NULL,
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_repos_tenant_repo UNIQUE (tenant_id, repo_full_name)
);

CREATE INDEX IF NOT EXISTS idx_repos_tenant_id ON repos (tenant_id);
CREATE INDEX IF NOT EXISTS idx_repos_installation_id ON repos (github_installation_id);

-- ─── Reviews ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS reviews (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    repo_id         UUID            NOT NULL REFERENCES repos(id) ON DELETE CASCADE,
    pr_number       INTEGER         NOT NULL,
    pr_title        VARCHAR(500),
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    tokens_used     INTEGER         DEFAULT 0,
    error_message   VARCHAR(1000),
    created_at      TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at    TIMESTAMP,

    CONSTRAINT chk_reviews_status CHECK (status IN ('PENDING', 'IN_PROGRESS', 'COMPLETED', 'FAILED'))
);

CREATE INDEX IF NOT EXISTS idx_reviews_repo_id ON reviews (repo_id);
CREATE INDEX IF NOT EXISTS idx_reviews_status ON reviews (status);
CREATE INDEX IF NOT EXISTS idx_reviews_created_at ON reviews (created_at);

-- ─── Review Comments ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS review_comments (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    review_id       UUID            NOT NULL REFERENCES reviews(id) ON DELETE CASCADE,
    file_path       VARCHAR(500)    NOT NULL,
    line_number     INTEGER         NOT NULL,
    severity        VARCHAR(10)     NOT NULL DEFAULT 'INFO',
    comment_body    VARCHAR(2000)   NOT NULL,

    CONSTRAINT chk_comments_severity CHECK (severity IN ('INFO', 'WARNING', 'ERROR'))
);

CREATE INDEX IF NOT EXISTS idx_review_comments_review_id ON review_comments (review_id);

-- ─── Usage Tracking ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS usage_tracking (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID            NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    month_year      VARCHAR(7)      NOT NULL,  -- Format: '2026-04'
    review_count    INTEGER         NOT NULL DEFAULT 0,
    token_count     BIGINT          NOT NULL DEFAULT 0,

    CONSTRAINT uq_usage_tenant_month UNIQUE (tenant_id, month_year)
);

CREATE INDEX IF NOT EXISTS idx_usage_tenant_id ON usage_tracking (tenant_id);
CREATE INDEX IF NOT EXISTS idx_usage_month_year ON usage_tracking (month_year);
