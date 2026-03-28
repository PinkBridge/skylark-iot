package cn.skylark.iot.access.protocol;

import lombok.Data;

@Data
public class ProtocolContext {
    private String traceId;
    private String topic;
    private String payload;
    private Long timestamp;
    private String productKey;
    private String deviceId;
    private String protocolType;
}

