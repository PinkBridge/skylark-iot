package cn.skylark.iot.mgmt.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeviceResponse {
    private String productKey;
    private String deviceKey;
    private String deviceName;
    private String address;
    private String deviceType;
    private String status;
    private String connectStatus;
    private LocalDateTime lastConnectedAt;
    private LocalDateTime lastDisconnectedAt;
    private String secret;
    private String protocolType;
    private String protocolVersion;
}

