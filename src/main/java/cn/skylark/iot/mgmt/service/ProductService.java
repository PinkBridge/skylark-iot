package cn.skylark.iot.mgmt.service;

import cn.skylark.iot.mgmt.model.dto.CreateProductRequest;
import cn.skylark.iot.mgmt.model.dto.ProductResponse;
import cn.skylark.iot.mgmt.model.dto.UpdateProductRequest;

import java.util.List;

public interface ProductService {

    ProductResponse create(CreateProductRequest req);

    ProductResponse get(String productKey);

    List<ProductResponse> list();

    ProductResponse update(String productKey, UpdateProductRequest req);

    ProductResponse enable(String productKey);

    ProductResponse disable(String productKey);
}
