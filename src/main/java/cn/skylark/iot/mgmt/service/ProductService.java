package cn.skylark.iot.mgmt.service;

import cn.skylark.iot.mgmt.model.dto.CreateProductRequest;
import cn.skylark.iot.mgmt.model.dto.CopyProductRequest;
import cn.skylark.iot.mgmt.model.dto.ProductPageQuery;
import cn.skylark.iot.mgmt.model.dto.ProductPageResponse;
import cn.skylark.iot.mgmt.model.dto.ProductResponse;
import cn.skylark.iot.mgmt.model.dto.UpdateProductRequest;

public interface ProductService {

    ProductResponse create(CreateProductRequest req);

    ProductResponse get(String productKey);

    ProductPageResponse list(ProductPageQuery query);

    ProductResponse update(String productKey, UpdateProductRequest req);

    ProductResponse enable(String productKey);

    ProductResponse disable(String productKey);

    ProductResponse copy(String productKey, CopyProductRequest req);

    void delete(String productKey);
}
