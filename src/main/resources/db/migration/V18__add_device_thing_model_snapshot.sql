CREATE TABLE IF NOT EXISTS iot_device_thing_model (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL DEFAULT 1,
    product_key VARCHAR(64) NOT NULL,
    device_key VARCHAR(64) NOT NULL,
    model_json TEXT NOT NULL,
    version VARCHAR(32) NOT NULL DEFAULT 'v1',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_iot_device_thing_model_tenant_pk_dk (tenant_id, product_key, device_key),
    KEY idx_iot_device_thing_model_tenant_product_device (tenant_id, product_key, device_key)
);
