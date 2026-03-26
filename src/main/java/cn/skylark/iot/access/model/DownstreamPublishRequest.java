package cn.skylark.iot.access.model;

import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;

@Data
public class DownstreamPublishRequest {

    /**
     * 链路追踪 ID（可由调用方传入；为空时由服务端生成并在响应头返回）。
     */
    private String traceId;

    @NotBlank(message = "topic cannot be empty")
    private String topic;

    /**
     * 下行 payload，MVP 先按字符串透传（可传 JSON 字符串）。
     */
    private String payload;

    @Min(value = 0, message = "qos must be 0,1,2")
    @Max(value = 2, message = "qos must be 0,1,2")
    private Integer qos = 1;

    private Boolean retain = false;
}

