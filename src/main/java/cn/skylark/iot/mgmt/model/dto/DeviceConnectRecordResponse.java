package cn.skylark.iot.mgmt.model.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DeviceConnectRecordResponse {
    private String action;
    private String clientId;
    private String ip;
    private String userAgent;
    private LocalDateTime createdAt;
}

