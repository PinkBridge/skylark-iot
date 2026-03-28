package cn.skylark.iot.mgmt.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeviceEntity {
    private Long id;
    private String productKey;
    private String deviceName;
    private String displayName;
    private String secret;
    private String status;
    private String protocolType;
    private String protocolVersion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

