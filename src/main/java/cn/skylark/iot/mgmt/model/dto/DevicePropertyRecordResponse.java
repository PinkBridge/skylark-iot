package cn.skylark.iot.mgmt.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DevicePropertyRecordResponse {
    private String propertyIdentifier;
    private String propertyValue;
    private String traceId;
    private String messageId;
    private String topic;
    private Long deviceTimestamp;
    private String payload;
    private LocalDateTime createdAt;
}
