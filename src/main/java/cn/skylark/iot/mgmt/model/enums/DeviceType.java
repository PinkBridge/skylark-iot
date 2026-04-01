package cn.skylark.iot.mgmt.model.enums;

public enum DeviceType {
    DIRECT_DEVICE,
    GATEWAY_DEVICE,
    GATEWAY_SUB_DEVICE,
    CAMERA;

    public static final DeviceType DEFAULT = DIRECT_DEVICE;

    public static boolean isValid(String value) {
        if (value == null || value.trim().isEmpty()) {
            return true;
        }
        for (DeviceType item : values()) {
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
        for (DeviceType item : values()) {
            if (item.name().equalsIgnoreCase(value.trim())) {
                return item.name();
            }
        }
        throw new IllegalArgumentException("unsupported device type");
    }
}
