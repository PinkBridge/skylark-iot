package cn.skylark.iot.mgmt.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProductPageResponse {
    private List<ProductResponse> records;
    private long total;
    private int pageNum;
    private int pageSize;
}
