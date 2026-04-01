package cn.skylark.iot.mgmt.model.dto;

import lombok.Data;

@Data
public class DeviceResponse {
    private String productKey;
    private String deviceName;
    private String displayName;
    private String deviceType;
    private String status;
    private String secret;
    private String protocolType;
    private String protocolVersion;
}

