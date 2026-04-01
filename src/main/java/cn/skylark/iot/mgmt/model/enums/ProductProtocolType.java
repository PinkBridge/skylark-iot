package cn.skylark.iot.mgmt.model.enums;

public enum ProductProtocolType {
    MQTT_ALINK_JSON,
    MQTT_CUSTOM;

    public static final ProductProtocolType DEFAULT = MQTT_ALINK_JSON;

    public static boolean isValid(String value) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        for (ProductProtocolType item : values()) {
            if (item.name().equalsIgnoreCase(value.trim())) {
                return true;
            }
        }
        return false;
    }

    public static String normalize(String value) {
        if (value == null || value.trim().isEmpty()) {
            return DEFAULT.name();
        }
        for (ProductProtocolType item : values()) {
            if (item.name().equalsIgnoreCase(value.trim())) {
                return item.name();
            }
        }
        throw new IllegalArgumentException("unsupported product protocol type");
    }
}
