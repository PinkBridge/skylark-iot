package cn.skylark.iot.mgmt.service;

import cn.skylark.iot.common.tenant.TenantContext;
import cn.skylark.iot.mgmt.mapper.DeviceRecordMapper;
import cn.skylark.iot.mgmt.mapper.DeviceMapper;
import cn.skylark.iot.mgmt.mapper.ProductMapper;
import cn.skylark.iot.mgmt.model.dto.CreateDeviceRequest;
import cn.skylark.iot.mgmt.model.dto.DeviceEventRecordPageResponse;
import cn.skylark.iot.mgmt.model.dto.DeviceEventRecordResponse;
import cn.skylark.iot.mgmt.model.dto.DevicePropertyRecordPageResponse;
import cn.skylark.iot.mgmt.model.dto.DevicePropertyRecordResponse;
import cn.skylark.iot.mgmt.model.dto.DeviceRecordPageQuery;
import cn.skylark.iot.mgmt.model.dto.DeviceResponse;
import cn.skylark.iot.mgmt.model.dto.DeviceServiceRecordPageResponse;
import cn.skylark.iot.mgmt.model.dto.DeviceServiceRecordResponse;
import cn.skylark.iot.mgmt.model.dto.UpdateDeviceRequest;
import cn.skylark.iot.mgmt.model.entity.DeviceEventRecordEntity;
import cn.skylark.iot.mgmt.model.entity.DeviceEntity;
import cn.skylark.iot.mgmt.model.entity.DevicePropertyRecordEntity;
import cn.skylark.iot.mgmt.model.entity.DeviceServiceRecordEntity;
import cn.skylark.iot.mgmt.model.entity.ProductEntity;
import cn.skylark.iot.mgmt.model.enums.DeviceType;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class DeviceServiceImpl implements DeviceService {
    private static final String STATUS_ENABLED = "enabled";
    private static final String STATUS_DISABLED = "disabled";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private final DeviceMapper deviceMapper;
    private final DeviceRecordMapper deviceRecordMapper;
    private final ProductMapper productMapper;

    public DeviceServiceImpl(DeviceMapper deviceMapper,
                             DeviceRecordMapper deviceRecordMapper,
                             ProductMapper productMapper) {
        this.deviceMapper = deviceMapper;
        this.deviceRecordMapper = deviceRecordMapper;
        this.productMapper = productMapper;
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
            entity.setDeviceKey(generateDeviceKey12());
            entity.setDeviceType(product.getDeviceType());
            entity.setSecret(generateSecret16());
            entity.setStatus(STATUS_ENABLED);
            entity.setProtocolType(product.getProtocolType());
            entity.setProtocolVersion("1.0");
            try {
                deviceMapper.insert(entity);
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
    public DeviceResponse update(String productKey, String deviceKey, UpdateDeviceRequest req) {
        String deviceType = DeviceType.normalize(req.getDeviceType());
        String protocolType = req.getProtocolType() != null && !req.getProtocolType().trim().isEmpty()
                ? req.getProtocolType().trim().toUpperCase(Locale.ROOT)
                : null;
        String protocolVersion = req.getProtocolVersion() != null && !req.getProtocolVersion().trim().isEmpty()
                ? req.getProtocolVersion().trim()
                : null;
        if (deviceMapper.updateProfile(productKey, deviceKey, deviceType, protocolType, protocolVersion) == 0) {
            throw new MgmtException(HttpStatus.NOT_FOUND, "device not found");
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
        String secret = generateSecret();
        if (deviceMapper.updateSecret(productKey, deviceKey, secret) == 0) {
            throw new MgmtException(HttpStatus.NOT_FOUND, "device not found");
        }
        return get(productKey, deviceKey);
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
            DevicePropertyRecordResponse response = new DevicePropertyRecordResponse();
            response.setPropertyIdentifier(item.getPropertyIdentifier());
            response.setPropertyValue(item.getPropertyValue());
            response.setTraceId(item.getTraceId());
            response.setMessageId(item.getMessageId());
            response.setTopic(item.getTopic());
            response.setDeviceTimestamp(item.getDeviceTimestamp());
            response.setPayload(item.getPayload());
            response.setCreatedAt(item.getCreatedAt());
            result.add(response);
        }
        DevicePropertyRecordPageResponse response = new DevicePropertyRecordPageResponse();
        response.setRecords(result);
        response.setTotal(deviceRecordMapper.countPropertyRecords(productKey, deviceKey));
        response.setPageNum(pageNum);
        response.setPageSize(pageSize);
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

    private DeviceResponse toResponse(DeviceEntity entity) {
        DeviceResponse resp = new DeviceResponse();
        resp.setProductKey(entity.getProductKey());
        resp.setDeviceKey(entity.getDeviceKey());
        resp.setDeviceName(entity.getDeviceName());
        resp.setDeviceType(entity.getDeviceType());
        resp.setStatus(entity.getStatus());
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
