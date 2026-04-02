CREATE TABLE IF NOT EXISTS iot_acl_policy (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_key VARCHAR(64) NOT NULL,
    subject_type VARCHAR(16) NOT NULL DEFAULT 'device',
    subject_value VARCHAR(64) NOT NULL,
    action VARCHAR(16) NOT NULL,
    topic_pattern VARCHAR(256) NOT NULL,
    effect VARCHAR(16) NOT NULL,
    priority INT NOT NULL DEFAULT 100,
    enabled TINYINT(1) NOT NULL DEFAULT 1,
    constraints_json TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

-- Some environments may already have an index with the original name.
-- Use a versioned name to avoid collision on migrate.
CREATE INDEX idx_iot_acl_policy_lookup_v2
    ON iot_acl_policy(product_key, action, subject_type, subject_value, enabled, priority);
