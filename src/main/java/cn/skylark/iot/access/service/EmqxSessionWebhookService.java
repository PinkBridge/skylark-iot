package cn.skylark.iot.access.service;

import cn.skylark.iot.access.mapper.AccessDeviceMapper;
import cn.skylark.iot.access.model.AccessDeviceRecord;
import cn.skylark.iot.access.model.EmqxClientSessionEvent;
import cn.skylark.iot.common.tenant.TenantContext;
import cn.skylark.iot.mgmt.model.dto.CreateDeviceConnectRecordRequest;
import cn.skylark.iot.mgmt.service.DeviceService;
import cn.skylark.iot.mgmt.service.MgmtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

/**
 * 将 EMQX 客户端会话事件落库：复用 {@link DeviceService#createConnectRecord}。
 */
@Service
public class EmqxSessionWebhookService {

    private static final Logger log = LoggerFactory.getLogger(EmqxSessionWebhookService.class);

    private static final String CONNECTED = "connected";
    private static final String DISCONNECTED = "disconnected";

    private final AccessDeviceMapper accessDeviceMapper;
    private final DeviceService deviceService;

    public EmqxSessionWebhookService(AccessDeviceMapper accessDeviceMapper,
                                    DeviceService deviceService) {
        this.accessDeviceMapper = accessDeviceMapper;
        this.deviceService = deviceService;
    }

    /**
     * @return false 表示已忽略（无需落库），true 表示已尝试落库（含失败仅打日志）
     */
    public boolean handleSessionEvent(EmqxClientSessionEvent body) {
        if (body == null) {
            return false;
        }
        String action = resolveAction(body);
        String deviceKey = safe(body.getUsername());
        if (!StringUtils.hasText(deviceKey)) {
            // Some disconnect events may miss username; fall back to clientid.
            deviceKey = safe(body.getClientid());
            if (StringUtils.hasText(deviceKey)) {
                log.info("emqx webhook fallback: username empty, use clientid as device_key={}", deviceKey);
            }
        }
        if (!StringUtils.hasText(deviceKey)) {
            log.warn("emqx webhook skipped: empty username/clientid (device_key)");
            return false;
        }

        List<AccessDeviceRecord> rows = accessDeviceMapper.findByDeviceKey(deviceKey);
        if (rows == null || rows.isEmpty()) {
            log.info("emqx webhook: no device for username/device_key={}", deviceKey);
            return false;
        }

        if (rows.size() > 1) {
            log.warn("emqx webhook: multiple iot_device rows for device_key={}, using first (product_key={})",
                    deviceKey, rows.get(0).getProductKey());
        }

        AccessDeviceRecord dev = rows.get(0);
        if (!deviceKey.equals(safe(dev.getDeviceKey()))) {
            log.warn("emqx webhook: row device_key mismatch");
            return false;
        }

        CreateDeviceConnectRecordRequest req = new CreateDeviceConnectRecordRequest();
        req.setAction(action);
        req.setClientId(trimToNull(body.getClientid(), 128));
        req.setIp(trimToNull(parseIpFromPeername(body.getPeername()), 64));
        req.setUserAgent(buildUserAgentNote(body, action));

        Long tenantId = dev.getTenantId();
        if (tenantId == null) {
            log.warn("emqx webhook: device row has null tenant_id for device_key={}", deviceKey);
            return false;
        }

        try {
            TenantContext.setTenantId(tenantId);
            try {
                deviceService.createConnectRecord(dev.getProductKey(), dev.getDeviceKey(), req);
                log.debug("emqx webhook recorded {} for {}/{}", action, dev.getProductKey(), dev.getDeviceKey());
            } finally {
                TenantContext.clear();
            }
        } catch (MgmtException e) {
            log.warn("emqx webhook createConnectRecord failed: {}", e.getMessage());
        } catch (RuntimeException e) {
            log.warn("emqx webhook createConnectRecord error", e);
        }
        return true;
    }

    private static String buildUserAgentNote(EmqxClientSessionEvent body, String action) {
        if (DISCONNECTED.equals(action) && StringUtils.hasText(body.getReason())) {
            String suffix = body.getNode() != null ? " node=" + body.getNode() : "";
            String note = "emqx-disconnect:" + body.getReason() + suffix;
            return trimToNull(note, 255);
        }
        if (StringUtils.hasText(body.getNode())) {
            return trimToNull("emqx:" + body.getNode(), 255);
        }
        return null;
    }

    private static String resolveAction(EmqxClientSessionEvent body) {
        String raw = safe(body.getEvent()).toLowerCase(Locale.ROOT);
        if (!raw.isEmpty()) {
            // 必须先判断 disconnect："client.disconnected" 也包含子串 "connect"
            if (raw.contains("disconnect")) {
                return DISCONNECTED;
            }
            if (raw.contains("connect")) {
                return CONNECTED;
            }
        }
        if (StringUtils.hasText(body.getReason())) {
            return DISCONNECTED;
        }
        // 规则只转发 client_connected 常用字段、未带 event 时，视为上线
        return CONNECTED;
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    /**
     * peername 常见为 IPv4 的 {@code ip:port}；尽力截取 IP。
     */
    static String parseIpFromPeername(String peername) {
        if (!StringUtils.hasText(peername)) {
            return "";
        }
        String p = peername.trim();
        if (p.startsWith("[") && p.contains("]:")) {
            int end = p.indexOf("]:");
            return p.substring(1, end);
        }
        int colon = p.lastIndexOf(':');
        if (colon > 0) {
            return p.substring(0, colon).trim();
        }
        return p;
    }

    private static String trimToNull(String s, int max) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        String t = s.trim();
        if (t.length() > max) {
            t = t.substring(0, max);
        }
        return t.isEmpty() ? null : t;
    }
}
