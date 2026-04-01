package cn.skylark.iot.mgmt.service;

import cn.skylark.iot.common.tenant.TenantContext;
import cn.skylark.iot.mgmt.mapper.ProductMapper;
import cn.skylark.iot.mgmt.mapper.ThingModelMapper;
import cn.skylark.iot.mgmt.model.dto.ThingModelResponse;
import cn.skylark.iot.mgmt.model.dto.UpsertThingModelRequest;
import cn.skylark.iot.mgmt.model.entity.ThingModelEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ThingModelServiceImpl implements ThingModelService {
    private final ThingModelMapper thingModelMapper;
    private final ProductMapper productMapper;
    private final ObjectMapper objectMapper;

    public ThingModelServiceImpl(ThingModelMapper thingModelMapper, ProductMapper productMapper, ObjectMapper objectMapper) {
        this.thingModelMapper = thingModelMapper;
        this.productMapper = productMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public ThingModelResponse upsert(String productKey, UpsertThingModelRequest request) {
        assertProductExists(productKey);
        String version = StringUtils.hasText(request.getVersion()) ? request.getVersion().trim() : "v1";
        validateModelJson(request.getModelJson());

        ThingModelEntity exists = thingModelMapper.findByPkAndVersion(productKey, version);
        if (exists == null) {
            ThingModelEntity entity = new ThingModelEntity();
            entity.setTenantId(TenantContext.getTenantId());
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

    private void validateModelJson(String modelJson) {
        try {
            JsonNode root = objectMapper.readTree(modelJson);
            if (root == null || !root.isObject()) {
                throw new MgmtException(HttpStatus.BAD_REQUEST, "thing model json must be an object");
            }
            validateSection(root, "properties");
            validateSection(root, "events");
            validateSection(root, "services");
        } catch (MgmtException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new MgmtException(HttpStatus.BAD_REQUEST, "thing model json is invalid");
        }
    }

    private void validateSection(JsonNode root, String fieldName) {
        JsonNode section = root.get(fieldName);
        if (section == null || !section.isArray()) {
            throw new MgmtException(HttpStatus.BAD_REQUEST, fieldName + " must be an array");
        }
        for (JsonNode item : section) {
            if (item == null || !item.isObject()) {
                throw new MgmtException(HttpStatus.BAD_REQUEST, fieldName + " item must be an object");
            }
            String identifier = item.path("identifier").asText("");
            if (!StringUtils.hasText(identifier)) {
                throw new MgmtException(HttpStatus.BAD_REQUEST, fieldName + " identifier cannot be empty");
            }
        }
    }

}
