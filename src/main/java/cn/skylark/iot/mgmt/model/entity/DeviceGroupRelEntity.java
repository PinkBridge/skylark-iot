package cn.skylark.iot.mgmt.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeviceGroupRelEntity {
    private Long id;
    private Long tenantId;
    private String groupKey;
    private String productKey;
    private String deviceKey;
    private LocalDateTime createdAt;
}

