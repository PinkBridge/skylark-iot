package cn.skylark.iot.access.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * EMQX 规则引擎 Webhook POST 体（来自 {@code $events/client_connected} / {@code $events/client_disconnected} 等）。
 * 未知字段忽略，便于不同 EMQX 版本扩展。
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class EmqxClientSessionEvent {
    /**
     * 部分集成会带 event 字段，如 {@code client.connected} / {@code client.disconnected}。
     */
    private String event;

    private String clientid;
    private String username;

    /**
     * 终端地址，通常为 {@code "ip:port"}。
     */
    private String peername;

    /**
     * 断开原因（仅下线事件）。
     */
    private String reason;

    private String node;
    private Long timestamp;
}
