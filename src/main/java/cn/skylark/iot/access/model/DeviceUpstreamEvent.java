package cn.skylark.iot.access.model;

import lombok.Data;

/**
 * 设备上行内部标准事件模型（接入层统一入口）。
 *
 * 约定：
 * - traceId：贯穿接入→存储/计算→下行（若外部未传入，由接入层生成）
 * - deviceId/messageType：尽量从 topic 推导补齐（如阿里物模型 /sys/...）
 * - payload：原始字符串透传（后续可在投递侧做 JSON 结构化/校验）
 */
@Data
public class DeviceUpstreamEvent {

    private String traceId;
    private String deviceId;
    private String messageType;
    private Long timestamp;
    private String topic;
    private String payload;
}

