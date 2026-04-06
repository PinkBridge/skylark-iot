package cn.skylark.iot.mgmt.service;

import cn.skylark.iot.access.mapper.AclPolicyMapper;
import cn.skylark.iot.access.model.AclPolicyRecord;
import cn.skylark.iot.common.tenant.TenantContext;
import cn.skylark.iot.mgmt.mapper.DeviceConnectRecordMapper;
import cn.skylark.iot.mgmt.mapper.DeviceRecordMapper;
import cn.skylark.iot.mgmt.mapper.DeviceThingModelMapper;
import cn.skylark.iot.mgmt.mapper.DeviceMapper;
import cn.skylark.iot.mgmt.mapper.ProductMapper;
import cn.skylark.iot.mgmt.mapper.ThingModelMapper;
import cn.skylark.iot.mgmt.model.dto.CreateDeviceConnectRecordRequest;
import cn.skylark.iot.mgmt.model.dto.CreateDeviceRequest;
import cn.skylark.iot.mgmt.model.dto.DeviceConnectRecordPageResponse;
import cn.skylark.iot.mgmt.model.dto.DeviceConnectRecordResponse;
import cn.skylark.iot.mgmt.model.dto.DeviceCurrentPropertyResponse;
import cn.skylark.iot.mgmt.model.dto.DeviceEventRecordPageResponse;
import cn.skylark.iot.mgmt.model.dto.DeviceEventRecordResponse;
import cn.skylark.iot.mgmt.model.dto.DevicePageQuery;
import cn.skylark.iot.mgmt.model.dto.DevicePageResponse;
import cn.skylark.iot.mgmt.model.dto.DevicePropertyRecordPageResponse;
import cn.skylark.iot.mgmt.model.dto.DevicePropertyRecordResponse;
import cn.skylark.iot.mgmt.model.dto.DeviceRecordPageQuery;
import cn.skylark.iot.mgmt.model.dto.DeviceResponse;
import cn.skylark.iot.mgmt.model.dto.DeviceServiceRecordPageResponse;
import cn.skylark.iot.mgmt.model.dto.DeviceServiceRecordResponse;
import cn.skylark.iot.mgmt.model.dto.ProductDataChannelResponse;
import cn.skylark.iot.mgmt.model.dto.UpdateProductDataChannelRequest;
import cn.skylark.iot.mgmt.model.dto.UpdateDeviceRequest;
import cn.skylark.iot.mgmt.model.entity.DeviceConnectRecordEntity;
import cn.skylark.iot.mgmt.model.entity.DeviceEventRecordEntity;
import cn.skylark.iot.mgmt.model.entity.DeviceEntity;
import cn.skylark.iot.mgmt.model.entity.DevicePropertyRecordEntity;
import cn.skylark.iot.mgmt.model.entity.DeviceServiceRecordEntity;
import cn.skylark.iot.mgmt.model.entity.DeviceThingModelEntity;
import cn.skylark.iot.mgmt.model.entity.ProductEntity;
import cn.skylark.iot.mgmt.model.entity.ThingModelEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class DeviceServiceImpl implements DeviceService {
    private static final Logger log = LoggerFactory.getLogger(DeviceServiceImpl.class);
    private static final String STATUS_ENABLED = "enabled";
    private static final String STATUS_DISABLED = "disabled";
    private static final String CONNECT_STATUS_DISCONNECTED = "disconnected";
    private static final String CONNECT_STATUS_CONNECTED = "connected";
    private static final String PROTOCOL_MQTT_ALINK_JSON = "MQTT_ALINK_JSON";
    private static final String DEVICE_KEY_PLACEHOLDER = "${deviceKey}";
    private static final Pattern DEVICE_KEY_PLACEHOLDER_PATTERN =
            Pattern.compile("\\$\\{\\s*device[_-]?key\\s*\\}|\\{\\s*device[_-]?key\\s*\\}", Pattern.CASE_INSENSITIVE);
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final DeviceMapper deviceMapper;
    private final DeviceRecordMapper deviceRecordMapper;
    private final DeviceConnectRecordMapper deviceConnectRecordMapper;
    private final ProductMapper productMapper;
    private final ThingModelMapper thingModelMapper;
    private final DeviceThingModelMapper deviceThingModelMapper;
    private final AclPolicyMapper aclPolicyMapper;
    private final ObjectMapper objectMapper;

    public DeviceServiceImpl(DeviceMapper deviceMapper,
                             DeviceRecordMapper deviceRecordMapper,
                             DeviceConnectRecordMapper deviceConnectRecordMapper,
                             ProductMapper productMapper,
                             ThingModelMapper thingModelMapper,
                             DeviceThingModelMapper deviceThingModelMapper,
                             AclPolicyMapper aclPolicyMapper,
                             ObjectMapper objectMapper) {
        this.deviceMapper = deviceMapper;
        this.deviceRecordMapper = deviceRecordMapper;
        this.deviceConnectRecordMapper = deviceConnectRecordMapper;
        this.productMapper = productMapper;
        this.thingModelMapper = thingModelMapper;
        this.deviceThingModelMapper = deviceThingModelMapper;
        this.aclPolicyMapper = aclPolicyMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public DeviceResponse create(String productKey, CreateDeviceRequest req) {
        ProductEntity product = getProduct(productKey);
        String deviceName = req.getDeviceName().trim();

        // Rarely, a short random deviceKey may collide. Retry a few times.
        for (int attempt = 0; attempt < 5; attempt++) {
            DeviceEntity entity = new DeviceEntity();
            entity.setTenantId(TenantContext.getTenantId());
            entity.setProductKey(productKey);
            entity.setDeviceName(deviceName);
            entity.setAddress(trimToNull(req.getAddress()));
            entity.setDeviceKey(generateDeviceKey12());
            entity.setDeviceType(product.getDeviceType());
            entity.setSecret(generateSecret16());
            entity.setStatus(STATUS_ENABLED);
            entity.setConnectStatus(CONNECT_STATUS_DISCONNECTED);
            entity.setProtocolType(product.getProtocolType());
            entity.setProtocolVersion("1.0");
            try {
                deviceMapper.insert(entity);
                initDeviceThingModelSnapshot(product, entity);
                initDefaultAclIfNeeded(product, entity);
                return get(productKey, entity.getDeviceKey());
            } catch (DuplicateKeyException e) {
                // If it's a deviceKey collision, retry; otherwise treat as conflict (e.g. duplicate deviceName).
                if (attempt >= 4) {
                    throw new MgmtException(HttpStatus.CONFLICT, "device already exists in this product");
                }
            }
        }
        throw new MgmtException(HttpStatus.CONFLICT, "device already exists in this product");
    }

    @Override
    public DeviceResponse get(String productKey, String deviceKey) {
        DeviceEntity entity = deviceMapper.findByPkAndDeviceKey(productKey, deviceKey);
        if (entity == null) {
            throw new MgmtException(HttpStatus.NOT_FOUND, "device not found");
        }
        return toResponse(entity);
    }

    @Override
    public List<DeviceResponse> list(String productKey) {
        assertProductExists(productKey);
        List<DeviceEntity> list = deviceMapper.listByProductKey(productKey);
        List<DeviceResponse> result = new ArrayList<DeviceResponse>();
        for (DeviceEntity item : list) {
            result.add(toResponse(item));
        }
        return result;
    }

    @Override
    public List<DeviceResponse> listAll() {
        List<DeviceEntity> list = deviceMapper.listAll();
        List<DeviceResponse> result = new ArrayList<DeviceResponse>();
        for (DeviceEntity item : list) {
            result.add(toResponse(item));
        }
        return result;
    }

    @Override
    public DevicePageResponse listAllPage(DevicePageQuery query) {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 20 : Math.min(query.getPageSize(), 100);
        int offset = (pageNum - 1) * pageSize;
        String keyword = StringUtils.hasText(query.getKeyword()) ? query.getKeyword().trim() : null;
        List<DeviceEntity> list = deviceMapper.listAllPage(keyword, offset, pageSize);
        long total = deviceMapper.countAllPage(keyword);
        List<DeviceResponse> records = new ArrayList<DeviceResponse>();
        for (DeviceEntity item : list) {
            records.add(toResponse(item));
        }
        DevicePageResponse response = new DevicePageResponse();
        response.setRecords(records);
        response.setTotal(total);
        response.setPageNum(pageNum);
        response.setPageSize(pageSize);
        return response;
    }

    @Override
    public DeviceResponse update(String productKey, String deviceKey, UpdateDeviceRequest req) {
        String name = req.getDeviceName() == null ? "" : req.getDeviceName().trim();
        if (name.isEmpty()) {
            throw new MgmtException(HttpStatus.BAD_REQUEST, "deviceName required");
        }
        String address = trimToNull(req.getAddress());
        try {
            if (deviceMapper.updateName(productKey, deviceKey, name, address) == 0) {
                throw new MgmtException(HttpStatus.NOT_FOUND, "device not found");
            }
        } catch (DuplicateKeyException e) {
            throw new MgmtException(HttpStatus.CONFLICT, "device name already exists in this product");
        }
        return get(productKey, deviceKey);
    }

    @Override
    public DeviceResponse enable(String productKey, String deviceKey) {
        return updateStatus(productKey, deviceKey, STATUS_ENABLED);
    }

    @Override
    public DeviceResponse disable(String productKey, String deviceKey) {
        return updateStatus(productKey, deviceKey, STATUS_DISABLED);
    }

    @Override
    public DeviceResponse resetSecret(String productKey, String deviceKey) {
        String secret = generateSecret16();
        if (deviceMapper.updateSecret(productKey, deviceKey, secret) == 0) {
            throw new MgmtException(HttpStatus.NOT_FOUND, "device not found");
        }
        return get(productKey, deviceKey);
    }

    @Override
    public List<ProductDataChannelResponse> listDataChannels(String productKey, String deviceKey) {
        assertDeviceExists(productKey, deviceKey);
        List<AclPolicyRecord> list = aclPolicyMapper.listDeviceChannels(productKey, deviceKey);
        List<ProductDataChannelResponse> result = new ArrayList<ProductDataChannelResponse>();
        if (list == null) {
            return result;
        }
        for (AclPolicyRecord item : list) {
            if (item == null) {
                continue;
            }
            ProductDataChannelResponse row = new ProductDataChannelResponse();
            row.setId(item.getId());
            row.setAction(item.getAction());
            row.setTopicPattern(item.getTopicPattern());
            row.setEffect(item.getEffect());
            row.setPriority(item.getPriority());
            row.setEnabled(Boolean.TRUE.equals(item.getEnabled()));
            result.add(row);
        }
        return result;
    }

    @Override
    public void updateDataChannel(String productKey, String deviceKey, Long id, UpdateProductDataChannelRequest request) {
        assertDeviceExists(productKey, deviceKey);
        if (id == null || id.longValue() <= 0) {
            throw new MgmtException(HttpStatus.BAD_REQUEST, "id invalid");
        }
        String effect = request.getEffect() == null ? "" : request.getEffect().trim().toLowerCase(Locale.ROOT);
        if (!"allow".equals(effect) && !"deny".equals(effect)) {
            throw new MgmtException(HttpStatus.BAD_REQUEST, "effect invalid");
        }
        if (request.getEnabled() == null) {
            throw new MgmtException(HttpStatus.BAD_REQUEST, "enabled required");
        }
        int updated = aclPolicyMapper.updateDeviceEffectAndEnabledById(id, productKey, deviceKey, effect, request.getEnabled());
        if (updated == 0) {
            throw new MgmtException(HttpStatus.NOT_FOUND, "data channel not found");
        }
    }

    @Override
    public List<DeviceCurrentPropertyResponse> listCurrentProperties(String productKey, String deviceKey) {
        assertDeviceExists(productKey, deviceKey);
        String modelJson = resolveDeviceThingModelJson(productKey, deviceKey);
        List<String> propertyIdentifiers = parsePropertyIdentifiers(modelJson);
        List<DeviceCurrentPropertyResponse> result = new ArrayList<DeviceCurrentPropertyResponse>();
        for (String identifier : propertyIdentifiers) {
            DeviceCurrentPropertyResponse row = new DeviceCurrentPropertyResponse();
            row.setPropertyIdentifier(identifier);
            DevicePropertyRecordEntity latest = deviceRecordMapper.findLatestPropertyRecord(productKey, deviceKey, identifier);
            if (latest != null) {
                row.setPropertyValue(latest.getPropertyValue());
                row.setDeviceTimestamp(latest.getDeviceTimestamp());
                row.setCreatedAt(latest.getCreatedAt());
            }
            result.add(row);
        }
        return result;
    }

    @Override
    public DevicePropertyRecordResponse getLatestPropertyValue(String productKey, String deviceKey, String propertyIdentifier) {
        assertDeviceExists(productKey, deviceKey);
        String identifier = safe(propertyIdentifier);
        if (!StringUtils.hasText(identifier)) {
            throw new MgmtException(HttpStatus.BAD_REQUEST, "propertyIdentifier required");
        }
        DevicePropertyRecordEntity entity = deviceRecordMapper.findLatestPropertyRecord(productKey, deviceKey, identifier);
        if (entity == null) {
            throw new MgmtException(HttpStatus.NOT_FOUND, "property record not found");
        }
        return toPropertyRecordResponse(entity);
    }

    @Override
    public DevicePropertyRecordPageResponse listPropertyRecords(String productKey, String deviceKey, DeviceRecordPageQuery query) {
        assertDeviceExists(productKey, deviceKey);
        int pageNum = normalizePageNum(query.getPageNum());
        int pageSize = normalizePageSize(query.getPageSize());
        int offset = (pageNum - 1) * pageSize;
        List<DevicePropertyRecordEntity> list = deviceRecordMapper.listPropertyRecords(productKey, deviceKey, offset, pageSize);
        List<DevicePropertyRecordResponse> result = new ArrayList<DevicePropertyRecordResponse>();
        for (DevicePropertyRecordEntity item : list) {
            result.add(toPropertyRecordResponse(item));
        }
        DevicePropertyRecordPageResponse response = new DevicePropertyRecordPageResponse();
        response.setRecords(result);
        response.setTotal(deviceRecordMapper.countPropertyRecords(productKey, deviceKey));
        response.setPageNum(pageNum);
        response.setPageSize(pageSize);
        return response;
    }

    private DevicePropertyRecordResponse toPropertyRecordResponse(DevicePropertyRecordEntity item) {
        DevicePropertyRecordResponse response = new DevicePropertyRecordResponse();
        response.setPropertyIdentifier(item.getPropertyIdentifier());
        response.setPropertyValue(item.getPropertyValue());
        response.setTraceId(item.getTraceId());
        response.setMessageId(item.getMessageId());
        response.setTopic(item.getTopic());
        response.setDeviceTimestamp(item.getDeviceTimestamp());
        response.setPayload(item.getPayload());
        response.setCreatedAt(item.getCreatedAt());
        return response;
    }

    @Override
    public DeviceEventRecordPageResponse listEventRecords(String productKey, String deviceKey, DeviceRecordPageQuery query) {
        assertDeviceExists(productKey, deviceKey);
        int pageNum = normalizePageNum(query.getPageNum());
        int pageSize = normalizePageSize(query.getPageSize());
        int offset = (pageNum - 1) * pageSize;
        List<DeviceEventRecordEntity> list = deviceRecordMapper.listEventRecords(productKey, deviceKey, offset, pageSize);
        List<DeviceEventRecordResponse> result = new ArrayList<DeviceEventRecordResponse>();
        for (DeviceEventRecordEntity item : list) {
            DeviceEventRecordResponse response = new DeviceEventRecordResponse();
            response.setEventName(item.getEventName());
            response.setTraceId(item.getTraceId());
            response.setMessageId(item.getMessageId());
            response.setTopic(item.getTopic());
            response.setDeviceTimestamp(item.getDeviceTimestamp());
            response.setPayload(item.getPayload());
            response.setCreatedAt(item.getCreatedAt());
            result.add(response);
        }
        DeviceEventRecordPageResponse response = new DeviceEventRecordPageResponse();
        response.setRecords(result);
        response.setTotal(deviceRecordMapper.countEventRecords(productKey, deviceKey));
        response.setPageNum(pageNum);
        response.setPageSize(pageSize);
        return response;
    }

    @Override
    public DeviceServiceRecordPageResponse listServiceRecords(String productKey, String deviceKey, DeviceRecordPageQuery query) {
        assertDeviceExists(productKey, deviceKey);
        int pageNum = normalizePageNum(query.getPageNum());
        int pageSize = normalizePageSize(query.getPageSize());
        int offset = (pageNum - 1) * pageSize;
        List<DeviceServiceRecordEntity> list = deviceRecordMapper.listServiceRecords(productKey, deviceKey, offset, pageSize);
        List<DeviceServiceRecordResponse> result = new ArrayList<DeviceServiceRecordResponse>();
        for (DeviceServiceRecordEntity item : list) {
            DeviceServiceRecordResponse response = new DeviceServiceRecordResponse();
            response.setServiceName(item.getServiceName());
            response.setDirection(item.getDirection());
            response.setTraceId(item.getTraceId());
            response.setMessageId(item.getMessageId());
            response.setTopic(item.getTopic());
            response.setDeviceTimestamp(item.getDeviceTimestamp());
            response.setPayload(item.getPayload());
            response.setOutputPayload(item.getOutputPayload());
            response.setCreatedAt(item.getCreatedAt());
            result.add(response);
        }
        DeviceServiceRecordPageResponse response = new DeviceServiceRecordPageResponse();
        response.setRecords(result);
        response.setTotal(deviceRecordMapper.countServiceRecords(productKey, deviceKey));
        response.setPageNum(pageNum);
        response.setPageSize(pageSize);
        return response;
    }

    @Override
    public void delete(String productKey, String deviceKey) {
        assertProductExists(productKey);
        if (deviceMapper.deleteByPkAndDeviceKey(productKey, deviceKey) == 0) {
            throw new MgmtException(HttpStatus.NOT_FOUND, "device not found");
        }
        deviceThingModelMapper.deleteByProductAndDevice(productKey, deviceKey);
    }

    @Override
    public DeviceConnectRecordPageResponse listConnectRecords(String productKey, String deviceKey, DeviceRecordPageQuery query) {
        assertDeviceExists(productKey, deviceKey);
        int pageNum = normalizePageNum(query.getPageNum());
        int pageSize = normalizePageSize(query.getPageSize());
        int offset = (pageNum - 1) * pageSize;
        List<DeviceConnectRecordEntity> list = deviceConnectRecordMapper.list(productKey, deviceKey, offset, pageSize);
        List<DeviceConnectRecordResponse> result = new ArrayList<DeviceConnectRecordResponse>();
        for (DeviceConnectRecordEntity item : list) {
            DeviceConnectRecordResponse resp = new DeviceConnectRecordResponse();
            resp.setAction(item.getAction());
            resp.setClientId(item.getClientId());
            resp.setIp(item.getIp());
            resp.setUserAgent(item.getUserAgent());
            resp.setCreatedAt(item.getCreatedAt());
            result.add(resp);
        }
        DeviceConnectRecordPageResponse response = new DeviceConnectRecordPageResponse();
        response.setRecords(result);
        response.setTotal(deviceConnectRecordMapper.count(productKey, deviceKey));
        response.setPageNum(pageNum);
        response.setPageSize(pageSize);
        return response;
    }

    @Override
    public void createConnectRecord(String productKey, String deviceKey, CreateDeviceConnectRecordRequest request) {
        assertDeviceExists(productKey, deviceKey);
        String action = request.getAction() == null ? "" : request.getAction().trim().toLowerCase(Locale.ROOT);
        if (!CONNECT_STATUS_CONNECTED.equals(action) && !CONNECT_STATUS_DISCONNECTED.equals(action)) {
            throw new MgmtException(HttpStatus.BAD_REQUEST, "action invalid");
        }
        int updated = deviceMapper.updateConnectStatusWithLastTime(productKey, deviceKey, action);
        if (updated == 0) {
            throw new MgmtException(HttpStatus.NOT_FOUND, "device not found");
        }
        DeviceConnectRecordEntity entity = new DeviceConnectRecordEntity();
        entity.setTenantId(TenantContext.getTenantId());
        entity.setProductKey(productKey);
        entity.setDeviceKey(deviceKey);
        entity.setAction(action);
        entity.setClientId(request.getClientId());
        entity.setIp(request.getIp());
        entity.setUserAgent(request.getUserAgent());
        deviceConnectRecordMapper.insert(entity);
    }

    private DeviceResponse updateStatus(String productKey, String deviceKey, String status) {
        if (deviceMapper.updateStatus(productKey, deviceKey, status) == 0) {
            throw new MgmtException(HttpStatus.NOT_FOUND, "device not found");
        }
        return get(productKey, deviceKey);
    }

    private void assertProductExists(String productKey) {
        if (productMapper.findByProductKey(productKey) == null) {
            throw new MgmtException(HttpStatus.NOT_FOUND, "product not found");
        }
    }

    private void assertDeviceExists(String productKey, String deviceKey) {
        if (deviceMapper.findByPkAndDeviceKey(productKey, deviceKey) == null) {
            throw new MgmtException(HttpStatus.NOT_FOUND, "device not found");
        }
    }

    private ProductEntity getProduct(String productKey) {
        ProductEntity product = productMapper.findByProductKey(productKey);
        if (product == null) {
            throw new MgmtException(HttpStatus.NOT_FOUND, "product not found");
        }
        return product;
    }

    private void initDeviceThingModelSnapshot(ProductEntity product, DeviceEntity device) {
        if (product == null || device == null) {
            return;
        }
        String productKey = safe(device.getProductKey());
        String deviceKey = safe(device.getDeviceKey());
        if (!StringUtils.hasText(productKey) || !StringUtils.hasText(deviceKey)) {
            return;
        }
        ThingModelEntity model = thingModelMapper.findLatestByProductKey(productKey);
        if (model == null || !StringUtils.hasText(model.getModelJson())) {
            return;
        }
        Long tenantId = device.getTenantId() == null ? TenantContext.getTenantId() : device.getTenantId();
        DeviceThingModelEntity snapshot = new DeviceThingModelEntity();
        snapshot.setTenantId(tenantId);
        snapshot.setProductKey(productKey);
        snapshot.setDeviceKey(deviceKey);
        snapshot.setVersion(safe(model.getVersion()));
        snapshot.setModelJson(model.getModelJson());
        try {
            deviceThingModelMapper.insert(snapshot);
        } catch (RuntimeException e) {
            // Keep create flow resilient if snapshot already exists (retry/import edge cases).
            deviceThingModelMapper.updateModel(productKey, deviceKey, snapshot.getVersion(), snapshot.getModelJson());
        }
    }

    private void initDefaultAclIfNeeded(ProductEntity product, DeviceEntity device) {
        if (product == null || device == null) {
            return;
        }
        if (!PROTOCOL_MQTT_ALINK_JSON.equalsIgnoreCase(safe(product.getProtocolType()))) {
            return;
        }
        String productKey = safe(device.getProductKey());
        String deviceKey = safe(device.getDeviceKey());
        if (!StringUtils.hasText(productKey) || !StringUtils.hasText(deviceKey)) {
            return;
        }
        Long tenantId = device.getTenantId() == null ? TenantContext.getTenantId() : device.getTenantId();

        List<AclPolicyRecord> templates = aclPolicyMapper.findProductTemplates(productKey);
        if (templates != null && !templates.isEmpty()) {
            for (AclPolicyRecord item : templates) {
                if (item == null || !Boolean.TRUE.equals(item.getEnabled())) {
                    continue;
                }
                String templateTopic = safe(item.getTopicPattern());
                if (!StringUtils.hasText(templateTopic)) {
                    continue;
                }
                String topicPattern = resolveDeviceTopicPattern(templateTopic, deviceKey);
                insertDefaultAclPolicy(tenantId, productKey, deviceKey,
                        safe(item.getAction()), topicPattern, safe(item.getEffect()),
                        item.getPriority() == null ? 200 : item.getPriority().intValue());
            }
            // Defensive pass: ensure imported/legacy template placeholders are fully materialized.
            aclPolicyMapper.replaceDevicePlaceholders(productKey, deviceKey);
            return;
        }

        // Backward-compatible fallback for older products without template policies.
        insertDefaultAclPolicy(tenantId, productKey, deviceKey, "publish",
                "/sys/" + productKey + "/" + deviceKey + "/thing/event/property/post", "allow", 300);
        insertDefaultAclPolicy(tenantId, productKey, deviceKey, "publish",
                "/sys/" + productKey + "/" + deviceKey + "/thing/event/+/post", "allow", 300);
        insertDefaultAclPolicy(tenantId, productKey, deviceKey, "publish",
                "/sys/" + productKey + "/" + deviceKey + "/thing/service/+/reply", "allow", 300);
        insertDefaultAclPolicy(tenantId, productKey, deviceKey, "publish",
                "/sys/" + productKey + "/" + deviceKey + "/thing/service/#", "deny", 200);
        insertDefaultAclPolicy(tenantId, productKey, deviceKey, "subscribe",
                "/sys/" + productKey + "/" + deviceKey + "/thing/service/#", "allow", 300);
        insertDefaultAclPolicy(tenantId, productKey, deviceKey, "subscribe",
                "/sys/" + productKey + "/" + deviceKey + "/thing/event/property/post_reply", "allow", 300);
        insertDefaultAclPolicy(tenantId, productKey, deviceKey, "subscribe",
                "/sys/" + productKey + "/" + deviceKey + "/thing/event/+/post_reply", "allow", 300);
        insertDefaultAclPolicy(tenantId, productKey, deviceKey, "subscribe",
                "/sys/" + productKey + "/" + deviceKey + "/thing/service/+/reply_ack", "allow", 300);
        aclPolicyMapper.replaceDevicePlaceholders(productKey, deviceKey);
    }

    private void insertDefaultAclPolicy(Long tenantId, String productKey, String deviceKey,
                                        String action, String topicPattern, String effect, int priority) {
        try {
            AclPolicyRecord policy = new AclPolicyRecord();
            policy.setTenantId(tenantId);
            policy.setProductKey(productKey);
            policy.setSubjectType("device");
            policy.setSubjectValue(deviceKey);
            policy.setAction(action);
            policy.setTopicPattern(topicPattern);
            policy.setEffect(effect);
            policy.setPriority(priority);
            policy.setEnabled(Boolean.TRUE);
            aclPolicyMapper.insert(policy);
        } catch (RuntimeException e) {
            log.warn("init default acl failed for {}/{} action={} topic={} effect={} priority={}: {}",
                    productKey, deviceKey, action, topicPattern, effect, priority, e.getMessage());
        }
    }

    private DeviceResponse toResponse(DeviceEntity entity) {
        DeviceResponse resp = new DeviceResponse();
        resp.setProductKey(entity.getProductKey());
        resp.setDeviceKey(entity.getDeviceKey());
        resp.setDeviceName(entity.getDeviceName());
        resp.setAddress(entity.getAddress());
        resp.setDeviceType(entity.getDeviceType());
        resp.setStatus(entity.getStatus());
        resp.setConnectStatus(entity.getConnectStatus());
        resp.setLastConnectedAt(entity.getLastConnectedAt());
        resp.setLastDisconnectedAt(entity.getLastDisconnectedAt());
        resp.setSecret(entity.getSecret());
        resp.setProtocolType(entity.getProtocolType());
        resp.setProtocolVersion(entity.getProtocolVersion());
        return resp;
    }

    private static String generateDeviceKey12() {
        // 12 hex chars = 6 bytes
        return randomHex(6);
    }

    private static String generateSecret16() {
        // 16 hex chars = 8 bytes
        return randomHex(8);
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String v = s.trim();
        return v.isEmpty() ? null : v;
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private String resolveDeviceThingModelJson(String productKey, String deviceKey) {
        DeviceThingModelEntity deviceModel = deviceThingModelMapper.findByProductAndDevice(productKey, deviceKey);
        if (deviceModel != null && StringUtils.hasText(deviceModel.getModelJson())) {
            return deviceModel.getModelJson();
        }
        ThingModelEntity productModel = thingModelMapper.findLatestByProductKey(productKey);
        if (productModel != null && StringUtils.hasText(productModel.getModelJson())) {
            return productModel.getModelJson();
        }
        return "{}";
    }

    private List<String> parsePropertyIdentifiers(String modelJson) {
        Set<String> unique = new LinkedHashSet<String>();
        try {
            JsonNode root = objectMapper.readTree(modelJson == null ? "{}" : modelJson);
            JsonNode props = root.path("properties");
            if (!props.isArray()) {
                return new ArrayList<String>();
            }
            for (JsonNode item : props) {
                if (item == null || !item.isObject()) {
                    continue;
                }
                String identifier = safe(item.path("identifier").asText(""));
                if (StringUtils.hasText(identifier)) {
                    unique.add(identifier);
                }
            }
        } catch (Exception e) {
            throw new MgmtException(HttpStatus.BAD_REQUEST, "device thing model json is invalid");
        }
        return new ArrayList<String>(unique);
    }

    private static String resolveDeviceTopicPattern(String templateTopic, String deviceKey) {
        String key = safe(deviceKey);
        if (!StringUtils.hasText(templateTopic) || !StringUtils.hasText(key)) {
            return safe(templateTopic);
        }
        // Backward compatible replacement:
        // ${deviceKey}, ${device_key}, {deviceKey}, {device_key} (case-insensitive, spaces tolerant).
        String replaced = DEVICE_KEY_PLACEHOLDER_PATTERN.matcher(templateTopic).replaceAll(key);
        // Keep original exact token replacement as a fallback for uncommon escaping cases.
        if (replaced.contains(DEVICE_KEY_PLACEHOLDER)) {
            replaced = replaced.replace(DEVICE_KEY_PLACEHOLDER, key);
        }
        return replaced;
    }

    private static String randomHex(int bytes) {
        byte[] buf = new byte[bytes];
        SECURE_RANDOM.nextBytes(buf);
        char[] out = new char[bytes * 2];
        final char[] HEX = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes; i++) {
            int v = buf[i] & 0xFF;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(out);
    }

    private int normalizePageNum(Integer pageNum) {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
    }
}
