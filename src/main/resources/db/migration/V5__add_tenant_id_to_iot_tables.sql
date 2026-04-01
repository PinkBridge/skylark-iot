ALTER TABLE iot_product
    ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 AFTER id;

ALTER TABLE iot_product
    DROP INDEX uk_iot_product_key,
    ADD UNIQUE KEY uk_iot_product_tenant_key (tenant_id, product_key),
    ADD INDEX idx_iot_product_tenant_id (tenant_id);

ALTER TABLE iot_device
    ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 AFTER id;

ALTER TABLE iot_device
    DROP INDEX uk_iot_device_pk_dn,
    ADD UNIQUE KEY uk_iot_device_tenant_pk_dn (tenant_id, product_key, device_name),
    ADD INDEX idx_iot_device_tenant_product_key (tenant_id, product_key),
    ADD INDEX idx_iot_device_tenant_device_name (tenant_id, device_name);

ALTER TABLE iot_thing_model
    ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 AFTER id;

ALTER TABLE iot_thing_model
    DROP INDEX uk_iot_thing_model_pk_ver,
    ADD UNIQUE KEY uk_iot_thing_model_tenant_pk_ver (tenant_id, product_key, version),
    ADD INDEX idx_iot_thing_model_tenant_product_key (tenant_id, product_key);

ALTER TABLE iot_acl_policy
    ADD COLUMN tenant_id BIGINT NOT NULL DEFAULT 1 AFTER id;

ALTER TABLE iot_acl_policy
    ADD INDEX idx_iot_acl_policy_tenant_id (tenant_id),
    ADD INDEX idx_iot_acl_policy_tenant_lookup (tenant_id, product_key, action, subject_type, subject_value, enabled, priority);
