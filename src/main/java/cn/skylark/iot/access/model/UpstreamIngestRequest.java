package cn.skylark.iot.access.model;

import lombok.Data;

@Data
public class UpstreamIngestRequest {

    /**
     * 链路追踪 ID（可由调用方传入；为空时由服务端生成）。
     */
    private String traceId;

    private String deviceId;

    private String messageType;

    /**
     * 设备侧时间戳（毫秒），可选。
     */
    private Long timestamp;

    /**
     * 原始 topic，可选。
     */
    private String topic;

    /**
     * 原始载荷，MVP 先以字符串接收。
     */
    private String payload;
}
