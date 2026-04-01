package cn.skylark.iot.access.model;

import lombok.Data;

@Data
public class AccessDeviceRecord {
    private Long tenantId;
    private String productKey;
    private String deviceName;
    private String secret;
    private String status;
    private String protocolType;
    private String protocolVersion;
}

