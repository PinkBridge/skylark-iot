package cn.skylark.iot.mgmt.controller;

import cn.skylark.iot.mgmt.model.dto.CreateProductRequest;
import cn.skylark.iot.mgmt.model.dto.CopyProductRequest;
import cn.skylark.iot.mgmt.model.dto.ProductPageQuery;
import cn.skylark.iot.mgmt.model.dto.ProductPageResponse;
import cn.skylark.iot.mgmt.model.dto.ProductResponse;
import cn.skylark.iot.mgmt.model.dto.UpdateProductRequest;
import cn.skylark.iot.mgmt.service.ProductService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mgmt/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ProductResponse create(@Validated @RequestBody CreateProductRequest request) {
        return productService.create(request);
    }

    @GetMapping
    public ProductPageResponse list(@ModelAttribute ProductPageQuery query) {
        return productService.list(query);
    }

    @GetMapping("/{productKey}")
    public ProductResponse get(@PathVariable("productKey") String productKey) {
        return productService.get(productKey);
    }

    @PutMapping("/{productKey}")
    public ProductResponse update(@PathVariable("productKey") String productKey,
                                  @Validated @RequestBody UpdateProductRequest request) {
        return productService.update(productKey, request);
    }

    @PatchMapping("/{productKey}/enable")
    public ProductResponse enable(@PathVariable("productKey") String productKey) {
        return productService.enable(productKey);
    }

    @PatchMapping("/{productKey}/disable")
    public ProductResponse disable(@PathVariable("productKey") String productKey) {
        return productService.disable(productKey);
    }

    @PostMapping("/{productKey}/copy")
    public ProductResponse copy(@PathVariable("productKey") String productKey,
                                @Validated @RequestBody CopyProductRequest request) {
        return productService.copy(productKey, request);
    }

    @DeleteMapping("/{productKey}")
    public void delete(@PathVariable("productKey") String productKey) {
        productService.delete(productKey);
    }
}

