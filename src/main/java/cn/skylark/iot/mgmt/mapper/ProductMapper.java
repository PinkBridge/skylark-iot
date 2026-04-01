package cn.skylark.iot.mgmt.mapper;

import cn.skylark.iot.mgmt.model.entity.ProductEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {
    int insert(ProductEntity entity);

    ProductEntity findByProductKey(@Param("productKey") String productKey);

    List<ProductEntity> listAll();

    int updateByProductKey(ProductEntity entity);

    int updateStatus(@Param("productKey") String productKey, @Param("status") String status);

    int deleteByProductKey(@Param("productKey") String productKey);
}

