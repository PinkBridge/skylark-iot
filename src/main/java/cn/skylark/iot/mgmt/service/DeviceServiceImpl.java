package cn.skylark.iot.mgmt.service;

import cn.skylark.iot.common.tenant.TenantContext;
import cn.skylark.iot.mgmt.mapper.DeviceMapper;
import cn.skylark.iot.mgmt.mapper.ProductMapper;
import cn.skylark.iot.mgmt.model.dto.CreateDeviceRequest;
import cn.skylark.iot.mgmt.model.dto.DeviceResponse;
import cn.skylark.iot.mgmt.model.dto.UpdateDeviceRequest;
import cn.skylark.iot.mgmt.model.entity.DeviceEntity;
import cn.skylark.iot.mgmt.model.entity.ProductEntity;
import cn.skylark.iot.mgmt.model.enums.DeviceType;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class DeviceServiceImpl implements DeviceService {
    private static final String STATUS_ENABLED = "enabled";
    private static final String STATUS_DISABLED = "disabled";

    private final DeviceMapper deviceMapper;
    private final ProductMapper productMapper;

    public DeviceServiceImpl(DeviceMapper deviceMapper, ProductMapper productMapper) {
        this.deviceMapper = deviceMapper;
        this.productMapper = productMapper;
    }

    @Override
    public DeviceResponse create(String productKey, CreateDeviceRequest req) {
        ProductEntity product = getProduct(productKey);
        DeviceEntity entity = new DeviceEntity();
        entity.setTenantId(TenantContext.getTenantId());
        entity.setProductKey(productKey);
        entity.setDeviceName(req.getDeviceName().trim());
        entity.setDisplayName(req.getDisplayName());
        entity.setDeviceType(product.getDeviceType());
        entity.setSecret(StringUtils.hasText(req.getSecret()) ? req.getSecret().trim() : generateSecret());
        entity.setStatus(STATUS_ENABLED);
        entity.setProtocolType(product.getProtocolType());
        entity.setProtocolVersion(StringUtils.hasText(req.getProtocolVersion()) ? req.getProtocolVersion().trim() : "1.0");
        try {
            deviceMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            throw new MgmtException(HttpStatus.CONFLICT, "device already exists in this product");
        }
        return get(productKey, entity.getDeviceName());
    }

    @Override
    public DeviceResponse get(String productKey, String deviceName) {
        DeviceEntity entity = deviceMapper.findByPkAndName(productKey, deviceName);
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
    public DeviceResponse update(String productKey, String deviceName, UpdateDeviceRequest req) {
        String deviceType = DeviceType.normalize(req.getDeviceType());
        String protocolType = StringUtils.hasText(req.getProtocolType())
                ? req.getProtocolType().trim().toUpperCase(Locale.ROOT)
                : null;
        String protocolVersion = StringUtils.hasText(req.getProtocolVersion())
                ? req.getProtocolVersion().trim()
                : null;
        if (deviceMapper.updateProfile(productKey, deviceName, req.getDisplayName(), deviceType, protocolType, protocolVersion) == 0) {
            throw new MgmtException(HttpStatus.NOT_FOUND, "device not found");
        }
        return get(productKey, deviceName);
    }

    @Override
    public DeviceResponse enable(String productKey, String deviceName) {
        return updateStatus(productKey, deviceName, STATUS_ENABLED);
    }

    @Override
    public DeviceResponse disable(String productKey, String deviceName) {
        return updateStatus(productKey, deviceName, STATUS_DISABLED);
    }

    @Override
    public DeviceResponse resetSecret(String productKey, String deviceName) {
        String secret = generateSecret();
        if (deviceMapper.updateSecret(productKey, deviceName, secret) == 0) {
            throw new MgmtException(HttpStatus.NOT_FOUND, "device not found");
        }
        return get(productKey, deviceName);
    }

    @Override
    public void delete(String productKey, String deviceName) {
        assertProductExists(productKey);
        if (deviceMapper.deleteByPkAndName(productKey, deviceName) == 0) {
            throw new MgmtException(HttpStatus.NOT_FOUND, "device not found");
        }
    }

    private DeviceResponse updateStatus(String productKey, String deviceName, String status) {
        if (deviceMapper.updateStatus(productKey, deviceName, status) == 0) {
            throw new MgmtException(HttpStatus.NOT_FOUND, "device not found");
        }
        return get(productKey, deviceName);
    }

    private void assertProductExists(String productKey) {
        if (getProduct(productKey) == null) {
            throw new MgmtException(HttpStatus.NOT_FOUND, "product not found");
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
        resp.setDeviceName(entity.getDeviceName());
        resp.setDisplayName(entity.getDisplayName());
        resp.setDeviceType(entity.getDeviceType());
        resp.setStatus(entity.getStatus());
        resp.setSecret(entity.getSecret());
        resp.setProtocolType(entity.getProtocolType());
        resp.setProtocolVersion(entity.getProtocolVersion());
        return resp;
    }

    private static String generateSecret() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
