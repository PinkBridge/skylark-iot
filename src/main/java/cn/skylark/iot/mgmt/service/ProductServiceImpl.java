package cn.skylark.iot.mgmt.service;

import cn.skylark.iot.common.tenant.TenantContext;
import cn.skylark.iot.mgmt.mapper.ProductMapper;
import cn.skylark.iot.mgmt.mapper.DeviceMapper;
import cn.skylark.iot.mgmt.mapper.ThingModelMapper;
import cn.skylark.iot.mgmt.model.dto.CreateProductRequest;
import cn.skylark.iot.mgmt.model.dto.ProductResponse;
import cn.skylark.iot.mgmt.model.dto.UpdateProductRequest;
import cn.skylark.iot.mgmt.model.entity.ProductEntity;
import cn.skylark.iot.mgmt.model.enums.DeviceType;
import cn.skylark.iot.mgmt.model.enums.ProductProtocolType;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {
    private static final String STATUS_ENABLED = "enabled";
    private static final String STATUS_DISABLED = "disabled";

    private final ProductMapper productMapper;
    private final DeviceMapper deviceMapper;
    private final ThingModelMapper thingModelMapper;

    public ProductServiceImpl(ProductMapper productMapper,
                              DeviceMapper deviceMapper,
                              ThingModelMapper thingModelMapper) {
        this.productMapper = productMapper;
        this.deviceMapper = deviceMapper;
        this.thingModelMapper = thingModelMapper;
    }

    @Override
    public ProductResponse create(CreateProductRequest req) {
        ProductEntity entity = new ProductEntity();
        entity.setTenantId(TenantContext.getTenantId());
        entity.setProductKey(req.getProductKey().trim());
        entity.setName(req.getName().trim());
        entity.setDescription(req.getDescription());
        entity.setProtocolType(ProductProtocolType.normalize(req.getProtocolType()));
        entity.setDeviceType(DeviceType.normalize(req.getDeviceType()));
        entity.setStatus(STATUS_ENABLED);
        try {
            productMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            throw new MgmtException(HttpStatus.CONFLICT, "productKey already exists");
        }
        return get(entity.getProductKey());
    }

    @Override
    public ProductResponse get(String productKey) {
        ProductEntity entity = productMapper.findByProductKey(productKey);
        if (entity == null) {
            throw new MgmtException(HttpStatus.NOT_FOUND, "product not found");
        }
        return toResponse(entity);
    }

    @Override
    public List<ProductResponse> list() {
        List<ProductEntity> all = productMapper.listAll();
        List<ProductResponse> result = new ArrayList<ProductResponse>();
        for (ProductEntity item : all) {
            result.add(toResponse(item));
        }
        return result;
    }

    @Override
    public ProductResponse update(String productKey, UpdateProductRequest req) {
        ProductEntity entity = new ProductEntity();
        entity.setProductKey(productKey);
        entity.setName(req.getName().trim());
        entity.setDescription(req.getDescription());
        entity.setProtocolType(ProductProtocolType.normalize(req.getProtocolType()));
        entity.setDeviceType(DeviceType.normalize(req.getDeviceType()));
        if (productMapper.updateByProductKey(entity) == 0) {
            throw new MgmtException(HttpStatus.NOT_FOUND, "product not found");
        }
        return get(productKey);
    }

    @Override
    public ProductResponse enable(String productKey) {
        return updateStatus(productKey, STATUS_ENABLED);
    }

    @Override
    public ProductResponse disable(String productKey) {
        return updateStatus(productKey, STATUS_DISABLED);
    }

    @Override
    public void delete(String productKey) {
        if (productMapper.findByProductKey(productKey) == null) {
            throw new MgmtException(HttpStatus.NOT_FOUND, "product not found");
        }
        deviceMapper.deleteByProductKey(productKey);
        thingModelMapper.deleteByProductKey(productKey);
        productMapper.deleteByProductKey(productKey);
    }

    private ProductResponse updateStatus(String productKey, String status) {
        if (productMapper.updateStatus(productKey, status) == 0) {
            throw new MgmtException(HttpStatus.NOT_FOUND, "product not found");
        }
        return get(productKey);
    }

    private ProductResponse toResponse(ProductEntity entity) {
        ProductResponse resp = new ProductResponse();
        resp.setProductKey(entity.getProductKey());
        resp.setName(entity.getName());
        resp.setDescription(entity.getDescription());
        resp.setProtocolType(entity.getProtocolType());
        resp.setDeviceType(entity.getDeviceType());
        resp.setStatus(entity.getStatus());
        return resp;
    }
}
