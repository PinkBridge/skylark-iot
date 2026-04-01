package cn.skylark.iot.mgmt.mapper;

import cn.skylark.iot.mgmt.model.entity.ThingModelEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ThingModelMapper {
    ThingModelEntity findByPkAndVersion(@Param("productKey") String productKey,
                                        @Param("version") String version);

    ThingModelEntity findLatestByProductKey(@Param("productKey") String productKey);

    int insert(ThingModelEntity entity);

    int updateModelJson(@Param("productKey") String productKey,
                        @Param("version") String version,
                        @Param("modelJson") String modelJson);

    int deleteByProductKey(@Param("productKey") String productKey);
}

