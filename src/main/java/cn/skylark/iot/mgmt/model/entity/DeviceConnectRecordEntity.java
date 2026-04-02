package cn.skylark.iot.mgmt.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeviceConnectRecordEntity {
    private Long id;
    private Long tenantId;
    private String productKey;
    private String deviceKey;
    private String action;
    private String clientId;
    private String ip;
    private String userAgent;
    private LocalDateTime createdAt;
}

