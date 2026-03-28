package cn.skylark.iot.mgmt.service;

import cn.skylark.iot.mgmt.mapper.ProductMapper;
import cn.skylark.iot.mgmt.mapper.ThingModelMapper;
import cn.skylark.iot.mgmt.model.dto.ThingModelResponse;
import cn.skylark.iot.mgmt.model.dto.UpsertThingModelRequest;
import cn.skylark.iot.mgmt.model.entity.ThingModelEntity;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ThingModelServiceImpl implements ThingModelService {
    private final ThingModelMapper thingModelMapper;
    private final ProductMapper productMapper;

    public ThingModelServiceImpl(ThingModelMapper thingModelMapper, ProductMapper productMapper) {
        this.thingModelMapper = thingModelMapper;
        this.productMapper = productMapper;
    }

    @Override
    public ThingModelResponse upsert(String productKey, UpsertThingModelRequest request) {
        assertProductExists(productKey);
        String version = StringUtils.hasText(request.getVersion()) ? request.getVersion().trim() : "v1";

        ThingModelEntity exists = thingModelMapper.findByPkAndVersion(productKey, version);
        if (exists == null) {
            ThingModelEntity entity = new ThingModelEntity();
            entity.setProductKey(productKey);
            entity.setVersion(version);
            entity.setModelJson(request.getModelJson());
            thingModelMapper.insert(entity);
        } else {
            thingModelMapper.updateModelJson(productKey, version, request.getModelJson());
        }
        return get(productKey);
    }

    @Override
    public ThingModelResponse get(String productKey) {
        ThingModelEntity entity = thingModelMapper.findLatestByProductKey(productKey);
        if (entity == null) {
            throw new MgmtException(HttpStatus.NOT_FOUND, "thing model not found");
        }
        ThingModelResponse response = new ThingModelResponse();
        response.setProductKey(entity.getProductKey());
        response.setVersion(entity.getVersion());
        response.setModelJson(entity.getModelJson());
        return response;
    }

    private void assertProductExists(String productKey) {
        if (productMapper.findByProductKey(productKey) == null) {
            throw new MgmtException(HttpStatus.NOT_FOUND, "product not found");
        }
    }
}
