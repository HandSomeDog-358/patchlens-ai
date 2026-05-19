CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    actor VARCHAR(255) NOT NULL,
    action VARCHAR(128) NOT NULL,
    resource_type VARCHAR(128) NOT NULL,
    resource_id VARCHAR(128),
    detail TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_logs_action ON audit_logs(action);
CREATE INDEX idx_audit_logs_resource ON audit_logs(resource_type, resource_id);
