package cn.skylark.iot.mgmt.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeviceCurrentPropertyResponse {
    private String propertyIdentifier;
    private String propertyValue;
    private Long deviceTimestamp;
    private LocalDateTime createdAt;
}

