package cn.skylark.iot.mgmt.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ThingModelEntity {
    private Long id;
    private Long tenantId;
    private String productKey;
    private String modelJson;
    private String version;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

