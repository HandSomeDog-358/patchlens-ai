CREATE TABLE platform_configs (
    id BIGSERIAL PRIMARY KEY,
    provider VARCHAR(32) NOT NULL UNIQUE,
    api_base_url VARCHAR(1000) NOT NULL,
    access_token_encrypted TEXT,
    webhook_secret_encrypted TEXT,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
