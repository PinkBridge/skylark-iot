CREATE TABLE IF NOT EXISTS iot_device_group (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    group_key VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_iot_device_group_tenant_gk (tenant_id, group_key),
    UNIQUE KEY uk_iot_device_group_tenant_name (tenant_id, name)
);

CREATE TABLE IF NOT EXISTS iot_device_group_rel (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    group_key VARCHAR(64) NOT NULL,
    product_key VARCHAR(64) NOT NULL,
    device_key VARCHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_iot_device_group_rel (tenant_id, group_key, product_key, device_key),
    KEY idx_iot_device_group_rel_lookup (tenant_id, group_key, created_at),
    KEY idx_iot_device_group_rel_device (tenant_id, product_key, device_key)
);

