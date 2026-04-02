package cn.skylark.iot.mgmt.service;

import cn.skylark.iot.common.tenant.TenantContext;
import cn.skylark.iot.mgmt.mapper.ProductMapper;
import cn.skylark.iot.mgmt.mapper.DeviceMapper;
import cn.skylark.iot.mgmt.mapper.ThingModelMapper;
import cn.skylark.iot.mgmt.model.dto.CopyProductRequest;
import cn.skylark.iot.mgmt.model.dto.CreateProductRequest;
import cn.skylark.iot.mgmt.model.dto.ProductPageQuery;
import cn.skylark.iot.mgmt.model.dto.ProductPageResponse;
import cn.skylark.iot.mgmt.model.dto.ProductResponse;
import cn.skylark.iot.mgmt.model.dto.UpdateProductRequest;
import cn.skylark.iot.mgmt.model.entity.ProductEntity;
import cn.skylark.iot.mgmt.model.entity.ThingModelEntity;
import cn.skylark.iot.mgmt.model.enums.DeviceType;
import cn.skylark.iot.mgmt.model.enums.ProductProtocolType;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@Service
public class ProductServiceImpl implements ProductService {
    private static final String STATUS_ENABLED = "enabled";
    private static final String STATUS_DISABLED = "disabled";
    private static final int PRODUCT_SECRET_LENGTH = 16;
    private static final char[] PRODUCT_SECRET_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

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
        entity.setProductSecret(generateProductSecret());
        entity.setName(req.getName().trim());
        entity.setCoverImageUrl(trimToNull(req.getCoverImageUrl()));
        entity.setThumbnailUrl(trimToNull(req.getThumbnailUrl()));
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
        return toResponse(entity, true);
    }

    @Override
    public ProductPageResponse list(ProductPageQuery query) {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : Math.min(query.getPageSize(), 100);
        int offset = (pageNum - 1) * pageSize;
        List<ProductEntity> list = productMapper.listPage(
                trimToNull(query.getProductKey()),
                trimToNull(query.getName()),
                trimToNull(query.getStatus()),
                offset,
                pageSize
        );
        List<ProductResponse> records = new ArrayList<ProductResponse>();
        for (ProductEntity item : list) {
            records.add(toResponse(item, false));
        }
        ProductPageResponse response = new ProductPageResponse();
        response.setRecords(records);
        response.setTotal(productMapper.countPage(
                trimToNull(query.getProductKey()),
                trimToNull(query.getName()),
                trimToNull(query.getStatus())
        ));
        response.setPageNum(pageNum);
        response.setPageSize(pageSize);
        return response;
    }

    @Override
    public ProductResponse update(String productKey, UpdateProductRequest req) {
        ProductEntity entity = new ProductEntity();
        entity.setProductKey(productKey);
        entity.setName(req.getName().trim());
        entity.setCoverImageUrl(trimToNull(req.getCoverImageUrl()));
        entity.setThumbnailUrl(trimToNull(req.getThumbnailUrl()));
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
    public ProductResponse copy(String productKey, CopyProductRequest req) {
        ProductEntity source = productMapper.findByProductKey(productKey);
        if (source == null) {
            throw new MgmtException(HttpStatus.NOT_FOUND, "product not found");
        }

        ProductEntity target = new ProductEntity();
        target.setTenantId(TenantContext.getTenantId());
        target.setProductKey(req.getTargetProductKey().trim());
        target.setProductSecret(generateProductSecret());
        target.setName(req.getTargetName().trim());
        target.setCoverImageUrl(source.getCoverImageUrl());
        target.setThumbnailUrl(source.getThumbnailUrl());
        target.setDescription(source.getDescription());
        target.setProtocolType(source.getProtocolType());
        target.setDeviceType(source.getDeviceType());
        target.setStatus(source.getStatus());
        try {
            productMapper.insert(target);
        } catch (DuplicateKeyException e) {
            throw new MgmtException(HttpStatus.CONFLICT, "productKey already exists");
        }

        ThingModelEntity thingModel = thingModelMapper.findLatestByProductKey(productKey);
        if (thingModel != null) {
            ThingModelEntity copiedThingModel = new ThingModelEntity();
            copiedThingModel.setTenantId(TenantContext.getTenantId());
            copiedThingModel.setProductKey(target.getProductKey());
            copiedThingModel.setVersion(thingModel.getVersion());
            copiedThingModel.setModelJson(thingModel.getModelJson());
            thingModelMapper.insert(copiedThingModel);
        }

        return get(target.getProductKey());
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

    private ProductResponse toResponse(ProductEntity entity, boolean includeSecret) {
        ProductResponse resp = new ProductResponse();
        resp.setProductKey(entity.getProductKey());
        if (includeSecret) {
            resp.setProductSecret(entity.getProductSecret());
        }
        resp.setName(entity.getName());
        resp.setCoverImageUrl(entity.getCoverImageUrl());
        resp.setThumbnailUrl(entity.getThumbnailUrl());
        resp.setDescription(entity.getDescription());
        resp.setProtocolType(entity.getProtocolType());
        resp.setDeviceType(entity.getDeviceType());
        resp.setStatus(entity.getStatus());
        resp.setDeviceCount(deviceMapper.countByProductKey(entity.getProductKey()));
        return resp;
    }

    private static String generateProductSecret() {
        char[] buf = new char[PRODUCT_SECRET_LENGTH];
        for (int i = 0; i < PRODUCT_SECRET_LENGTH; i++) {
            buf[i] = PRODUCT_SECRET_ALPHABET[SECURE_RANDOM.nextInt(PRODUCT_SECRET_ALPHABET.length)];
        }
        return new String(buf);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
