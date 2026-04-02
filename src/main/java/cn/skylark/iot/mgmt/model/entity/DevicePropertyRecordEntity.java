package cn.skylark.iot.mgmt.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DevicePropertyRecordEntity {
    private Long id;
    private Long tenantId;
    private String productKey;
    private String deviceName;
    private String propertyIdentifier;
    private String propertyValue;
    private String traceId;
    private String messageId;
    private String topic;
    private Long deviceTimestamp;
    private String payload;
    private LocalDateTime createdAt;
}
