package cn.skylark.iot.mgmt.model.dto;

import lombok.Data;

@Data
public class ProductPageQuery {
    private String productKey;
    private String name;
    private String status;
    private Integer pageNum = 1;
    private Integer pageSize = 10;
}
