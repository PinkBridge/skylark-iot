package cn.skylark.iot.access.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.skylark.iot.access.model.DownstreamPublishRequest;
import cn.skylark.iot.access.service.EmqxManagementClient;
import cn.skylark.iot.mgmt.mapper.DeviceRecordMapper;
import cn.skylark.iot.mgmt.mapper.ProductMapper;
import cn.skylark.iot.mgmt.model.entity.DeviceServiceRecordEntity;
import cn.skylark.iot.mgmt.model.entity.ProductEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.UUID;

/**
 * 下行接口：设备管理/应用使能调用此接口，由 iot-access 通过 EMQX Management API 执行 publish。
 */
@RestController
@RequestMapping("/api/access/downstream")
public class DownstreamController {
    private static final Pattern SERVICE_INVOKE_TOPIC = Pattern.compile("^/sys/([^/]+)/([^/]+)/thing/service/([^/]+)/invoke$", Pattern.CASE_INSENSITIVE);

    private final EmqxManagementClient emqxManagementClient;
    private final DeviceRecordMapper deviceRecordMapper;
    private final ProductMapper productMapper;
    private final ObjectMapper objectMapper;

    public DownstreamController(EmqxManagementClient emqxManagementClient,
                                DeviceRecordMapper deviceRecordMapper,
                                ProductMapper productMapper,
                                ObjectMapper objectMapper) {
        this.emqxManagementClient = emqxManagementClient;
        this.deviceRecordMapper = deviceRecordMapper;
        this.productMapper = productMapper;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/publish")
    public ResponseEntity<String> publish(@Validated @RequestBody DownstreamPublishRequest request) {
        if (request.getTraceId() == null || request.getTraceId().trim().isEmpty()) {
            request.setTraceId(UUID.randomUUID().toString().replace("-", ""));
        }
        persistServiceInvokeRecord(request);
        return emqxManagementClient.publish(request)
                .map((err) -> ResponseEntity.status(502).body(err))
                .orElseGet(() -> ResponseEntity.ok()
                        .header("X-Trace-Id", request.getTraceId())
                        .body("ok"));
    }

    private void persistServiceInvokeRecord(DownstreamPublishRequest request) {
        Matcher matcher = SERVICE_INVOKE_TOPIC.matcher(request.getTopic() == null ? "" : request.getTopic().trim());
        if (!matcher.matches()) {
            return;
        }
        DeviceServiceRecordEntity record = new DeviceServiceRecordEntity();
        record.setProductKey(matcher.group(1));
        record.setDeviceName(matcher.group(2));
        record.setServiceName(resolveServiceName(request, matcher.group(3)));
        record.setDirection("request");
        record.setTraceId(request.getTraceId());
        record.setMessageId(resolveMessageId(request));
        record.setTopic(request.getTopic());
        record.setPayload(request.getPayload());
        record.setTenantId(resolveTenantId(record.getProductKey()));
        deviceRecordMapper.insertServiceRecord(record);
    }

    private String resolveServiceName(DownstreamPublishRequest request, String fallback) {
        try {
            JsonNode root = objectMapper.readTree(request.getPayload() == null ? "" : request.getPayload());
            if (root.has("method")) {
                String method = root.path("method").asText("");
                String lower = method.toLowerCase(Locale.ROOT);
                if (lower.startsWith("thing.service.") && lower.endsWith(".invoke")) {
                    return method.substring("thing.service.".length(), method.length() - ".invoke".length());
                }
            }
        } catch (Exception ignored) {
            // Ignore payload parsing issues and fall back to topic-derived service name.
        }
        return fallback;
    }

    private String resolveMessageId(DownstreamPublishRequest request) {
        try {
            JsonNode root = objectMapper.readTree(request.getPayload() == null ? "" : request.getPayload());
            return root.has("id") ? root.path("id").asText(null) : null;
        } catch (Exception ignored) {
            return null;
        }
    }

    private Long resolveTenantId(String productKey) {
        ProductEntity product = productMapper.findByProductKey(productKey);
        return product == null ? 1L : product.getTenantId();
    }
}

