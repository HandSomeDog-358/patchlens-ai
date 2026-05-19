CREATE TABLE repositories (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(32) NOT NULL,
    owner VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    default_branch VARCHAR(255) NOT NULL DEFAULT 'main',
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uk_repositories_provider_owner_name UNIQUE (provider, owner, name)
);

CREATE TABLE review_policies (
    id BIGSERIAL PRIMARY KEY,
    repository_id BIGINT NOT NULL UNIQUE REFERENCES repositories(id) ON DELETE CASCADE,
    language VARCHAR(32) NOT NULL DEFAULT 'zh-CN',
    min_confidence DOUBLE PRECISION NOT NULL DEFAULT 0.75,
    max_inline_comments INTEGER NOT NULL DEFAULT 5,
    enable_summary BOOLEAN NOT NULL DEFAULT TRUE,
    enable_inline_comments BOOLEAN NOT NULL DEFAULT TRUE,
    enable_suggested_patch BOOLEAN NOT NULL DEFAULT TRUE,
    ignored_paths TEXT,
    focus_paths TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE review_tasks (
    id BIGSERIAL PRIMARY KEY,
    repository_id BIGINT NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    provider VARCHAR(32) NOT NULL,
    pr_number INTEGER NOT NULL,
    commit_sha VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    trigger_type VARCHAR(32) NOT NULL,
    summary TEXT,
    started_at TIMESTAMPTZ,
    finished_at TIMESTAMPTZ,
    error_message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_review_tasks_repository_pr ON review_tasks(repository_id, pr_number);
CREATE INDEX idx_review_tasks_status ON review_tasks(status);

CREATE TABLE review_findings (
    id BIGSERIAL PRIMARY KEY,
    review_task_id BIGINT NOT NULL REFERENCES review_tasks(id) ON DELETE CASCADE,
    severity VARCHAR(32) NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    line_number INTEGER NOT NULL,
    title VARCHAR(500) NOT NULL,
    description TEXT,
    evidence TEXT,
    suggestion TEXT,
    patch TEXT,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    provider_comment_id VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_review_findings_task ON review_findings(review_task_id);
CREATE INDEX idx_review_findings_severity ON review_findings(severity);

CREATE TABLE review_feedback (
    id BIGSERIAL PRIMARY KEY,
    finding_id BIGINT NOT NULL REFERENCES review_findings(id) ON DELETE CASCADE,
    value VARCHAR(64) NOT NULL,
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE model_configs (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(64) NOT NULL,
    base_url VARCHAR(1000) NOT NULL,
    model_name VARCHAR(255) NOT NULL,
    api_key_encrypted TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE EXTENSION IF NOT EXISTS vector;

CREATE TABLE code_chunks (
    id BIGSERIAL PRIMARY KEY,
    repository_id BIGINT NOT NULL REFERENCES repositories(id) ON DELETE CASCADE,
    branch VARCHAR(255) NOT NULL,
    commit_sha VARCHAR(128) NOT NULL,
    file_path VARCHAR(1000) NOT NULL,
    language VARCHAR(64),
    symbol_name VARCHAR(500),
    start_line INTEGER NOT NULL,
    end_line INTEGER NOT NULL,
    content_hash VARCHAR(128) NOT NULL,
    content TEXT NOT NULL,
    embedding vector(1536),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_code_chunks_repository_file ON code_chunks(repository_id, file_path);
