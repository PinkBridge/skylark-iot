package cn.skylark.iot.mgmt.model.dto;

import lombok.Data;

@Data
public class ThingModelResponse {
    private String productKey;
    private String version;
    private String modelJson;
}

