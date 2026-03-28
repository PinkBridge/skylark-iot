CREATE TABLE IF NOT EXISTS iot_product (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_key VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(512),
    status VARCHAR(16) NOT NULL DEFAULT 'enabled',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_iot_product_key (product_key)
);

CREATE TABLE IF NOT EXISTS iot_device (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_key VARCHAR(64) NOT NULL,
    device_name VARCHAR(64) NOT NULL,
    display_name VARCHAR(128),
    secret VARCHAR(256) NOT NULL,
    status VARCHAR(16) NOT NULL DEFAULT 'enabled',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_iot_device_pk_dn (product_key, device_name)
);

CREATE TABLE IF NOT EXISTS iot_thing_model (
    id BIGINT NOT NULL AUTO_INCREMENT,
    product_key VARCHAR(64) NOT NULL,
    model_json TEXT NOT NULL,
    version VARCHAR(32) NOT NULL DEFAULT 'v1',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_iot_thing_model_pk_ver (product_key, version)
);

CREATE INDEX idx_iot_device_product_key ON iot_device(product_key);
CREATE INDEX idx_iot_thing_model_product_key ON iot_thing_model(product_key);
