package cn.skylark.iot.mgmt.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductEntity {
    private Long id;
    private Long tenantId;
    private String productKey;
    private String name;
    private String description;
    private String protocolType;
    private String deviceType;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}

