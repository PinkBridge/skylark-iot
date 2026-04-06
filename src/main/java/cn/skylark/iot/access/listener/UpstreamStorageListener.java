package cn.skylark.iot.access.listener;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cn.skylark.iot.access.event.DeviceUpstreamEventPublished;
import cn.skylark.iot.access.model.DeviceUpstreamEvent;
import cn.skylark.iot.mgmt.mapper.DeviceRecordMapper;
import cn.skylark.iot.mgmt.mapper.ProductMapper;
import cn.skylark.iot.mgmt.model.entity.DeviceEventRecordEntity;
import cn.skylark.iot.mgmt.model.entity.DevicePropertyRecordEntity;
import cn.skylark.iot.mgmt.model.entity.DeviceServiceRecordEntity;
import cn.skylark.iot.mgmt.model.entity.ProductEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Iterator;
import java.util.Map;

/**
 * 落库 listener（占位实现）。
 * 后续可在这里对接 MySQL/TimescaleDB，把上行事件异步写入。
 */
@Component
public class UpstreamStorageListener {

    private static final Logger log = LoggerFactory.getLogger(UpstreamStorageListener.class);
    private final ObjectMapper objectMapper;
    private final DeviceRecordMapper deviceRecordMapper;
    private final ProductMapper productMapper;

    public UpstreamStorageListener(ObjectMapper objectMapper,
                                   DeviceRecordMapper deviceRecordMapper,
                                   ProductMapper productMapper) {
        this.objectMapper = objectMapper;
        this.deviceRecordMapper = deviceRecordMapper;
        this.productMapper = productMapper;
    }

    @Async("iotEventExecutor")
    @EventListener
    public void on(DeviceUpstreamEventPublished published) {
        if (published == null || published.getEvent() == null) {
            return;
        }
        DeviceUpstreamEvent e = published.getEvent();
        if (!StringUtils.hasText(e.getProductKey()) || !StringUtils.hasText(e.getDeviceId())) {
            return;
        }
        try {
            if ("PROPERTY_POST".equals(e.getEventType())) {
                persistPropertyRecords(e);
                return;
            }
            if ("EVENT_POST".equals(e.getEventType())) {
                persistEventRecord(e);
                return;
            }
            if ("SERVICE_REPLY".equals(e.getEventType())) {
                persistServiceReplyRecord(e);
            }
        } catch (Exception ex) {
            log.warn("iot.upstream.storage.failed traceId={}, deviceId={}, messageType={}",
                    e.getTraceId(), e.getDeviceId(), e.getMessageType(), ex);
        }
    }

    private void persistPropertyRecords(DeviceUpstreamEvent event) throws Exception {
        JsonNode root = objectMapper.readTree(safe(event.getPayload()));
        JsonNode params = root.path("params");
        if (!params.isObject()) {
            return;
        }
        Iterator<Map.Entry<String, JsonNode>> fields = params.fields();
        Long tenantId = resolveTenantId(event.getProductKey());
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            DevicePropertyRecordEntity record = new DevicePropertyRecordEntity();
            record.setTenantId(tenantId);
            record.setProductKey(event.getProductKey());
            record.setDeviceName(event.getDeviceId());
            record.setPropertyIdentifier(field.getKey());
            record.setPropertyValue(stringify(field.getValue()));
            record.setTraceId(event.getTraceId());
            record.setMessageId(event.getMessageId());
            record.setTopic(event.getTopic());
            record.setDeviceTimestamp(resolveDeviceTimestamp(root, event));
            record.setPayload(safe(event.getPayload()));
            deviceRecordMapper.insertPropertyRecord(record);
        }
    }

    private void persistEventRecord(DeviceUpstreamEvent event) throws Exception {
        JsonNode root = objectMapper.readTree(safe(event.getPayload()));
        DeviceEventRecordEntity record = new DeviceEventRecordEntity();
        record.setTenantId(resolveTenantId(event.getProductKey()));
        record.setProductKey(event.getProductKey());
        record.setDeviceName(event.getDeviceId());
        record.setEventName(StringUtils.hasText(event.getEventName()) ? event.getEventName() : event.getMessageType());
        record.setTraceId(event.getTraceId());
        record.setMessageId(event.getMessageId());
        record.setTopic(event.getTopic());
        record.setDeviceTimestamp(resolveDeviceTimestamp(root, event));
        record.setPayload(stringifyPayloadNode(root.path("params"), event.getPayload()));
        deviceRecordMapper.insertEventRecord(record);
    }

    private void persistServiceReplyRecord(DeviceUpstreamEvent event) throws Exception {
        JsonNode root = objectMapper.readTree(safe(event.getPayload()));
        String outputPayload = stringifyPayloadNode(root.path("data"), event.getPayload());
        int updated = deviceRecordMapper.updateServiceRecordOutputByMessageId(
                event.getProductKey(),
                event.getDeviceId(),
                StringUtils.hasText(event.getServiceName()) ? event.getServiceName() : "unknown",
                event.getMessageId(),
                event.getTopic(),
                resolveDeviceTimestamp(root, event),
                outputPayload
        );
        if (updated > 0) {
            return;
        }
        DeviceServiceRecordEntity record = new DeviceServiceRecordEntity();
        record.setTenantId(resolveTenantId(event.getProductKey()));
        record.setProductKey(event.getProductKey());
        record.setDeviceName(event.getDeviceId());
        record.setServiceName(StringUtils.hasText(event.getServiceName()) ? event.getServiceName() : "unknown");
        record.setDirection("reply");
        record.setTraceId(event.getTraceId());
        record.setMessageId(event.getMessageId());
        record.setTopic(event.getTopic());
        record.setDeviceTimestamp(resolveDeviceTimestamp(root, event));
        record.setPayload(outputPayload);
        deviceRecordMapper.insertServiceRecord(record);
    }

    private Long resolveTenantId(String productKey) {
        ProductEntity product = productMapper.findByProductKey(productKey);
        return product == null ? 1L : product.getTenantId();
    }

    private Long resolveDeviceTimestamp(JsonNode root, DeviceUpstreamEvent event) {
        if (root != null && root.has("timestamp") && root.path("timestamp").canConvertToLong()) {
            return root.path("timestamp").asLong();
        }
        return event.getTimestamp();
    }

    private String stringifyPayloadNode(JsonNode node, String fallback) throws Exception {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return safe(fallback);
        }
        return stringify(node);
    }

    private String stringify(JsonNode node) throws Exception {
        if (node == null || node.isNull()) {
            return null;
        }
        if (node.isValueNode()) {
            return node.asText();
        }
        return objectMapper.writeValueAsString(node);
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }
}

