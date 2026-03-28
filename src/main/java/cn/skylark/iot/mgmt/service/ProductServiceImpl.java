package cn.skylark.iot.mgmt.service;

import cn.skylark.iot.mgmt.mapper.ProductMapper;
import cn.skylark.iot.mgmt.model.dto.CreateProductRequest;
import cn.skylark.iot.mgmt.model.dto.ProductResponse;
import cn.skylark.iot.mgmt.model.dto.UpdateProductRequest;
import cn.skylark.iot.mgmt.model.entity.ProductEntity;
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

    public ProductServiceImpl(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Override
    public ProductResponse create(CreateProductRequest req) {
        ProductEntity entity = new ProductEntity();
        entity.setProductKey(req.getProductKey().trim());
        entity.setName(req.getName().trim());
        entity.setDescription(req.getDescription());
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
        resp.setStatus(entity.getStatus());
        return resp;
    }
}
