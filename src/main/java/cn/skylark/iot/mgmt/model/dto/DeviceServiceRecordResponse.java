package cn.skylark.iot.mgmt.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeviceServiceRecordResponse {
    private String serviceName;
    private String direction;
    private String traceId;
    private String messageId;
    private String topic;
    private Long deviceTimestamp;
    private String payload;
    private String outputPayload;
    private LocalDateTime createdAt;
}
