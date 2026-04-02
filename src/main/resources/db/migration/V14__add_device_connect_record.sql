CREATE TABLE IF NOT EXISTS iot_device_connect_record (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    product_key VARCHAR(64) NOT NULL,
    device_key VARCHAR(64) NOT NULL,
    action VARCHAR(16) NOT NULL,
    client_id VARCHAR(128),
    ip VARCHAR(64),
    user_agent VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_iot_connect_record_lookup (tenant_id, product_key, device_key, created_at)
);

