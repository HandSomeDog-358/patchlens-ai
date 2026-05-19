ALTER TABLE review_tasks
    ADD COLUMN conclusion VARCHAR(32),
    ADD COLUMN published_at TIMESTAMPTZ,
    ADD COLUMN publish_error TEXT;
