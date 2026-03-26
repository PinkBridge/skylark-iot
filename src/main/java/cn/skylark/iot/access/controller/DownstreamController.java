package cn.skylark.iot.access.controller;

import cn.skylark.iot.access.model.DownstreamPublishRequest;
import cn.skylark.iot.access.service.EmqxManagementClient;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 下行接口：设备管理/应用使能调用此接口，由 iot-access 通过 EMQX Management API 执行 publish。
 */
@RestController
@RequestMapping("/api/access/downstream")
public class DownstreamController {

    private final EmqxManagementClient emqxManagementClient;

    public DownstreamController(EmqxManagementClient emqxManagementClient) {
        this.emqxManagementClient = emqxManagementClient;
    }

    @PostMapping("/publish")
    public ResponseEntity<String> publish(@Validated @RequestBody DownstreamPublishRequest request) {
        if (request.getTraceId() == null || request.getTraceId().trim().isEmpty()) {
            request.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        }
        return emqxManagementClient.publish(request)
                .map((err) -> ResponseEntity.status(502).body(err))
                .orElseGet(() -> ResponseEntity.ok()
                        .header("X-Trace-Id", request.getTraceId())
                        .body("ok"));
    }
}

