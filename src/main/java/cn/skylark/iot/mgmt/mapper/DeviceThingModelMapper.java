package cn.skylark.iot.mgmt.mapper;

import cn.skylark.iot.mgmt.model.entity.DeviceThingModelEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface DeviceThingModelMapper {
    int insert(DeviceThingModelEntity entity);

    int updateModel(@Param("productKey") String productKey,
                    @Param("deviceKey") String deviceKey,
                    @Param("version") String version,
                    @Param("modelJson") String modelJson);

    DeviceThingModelEntity findByProductAndDevice(@Param("productKey") String productKey,
                                                  @Param("deviceKey") String deviceKey);

    int deleteByProductAndDevice(@Param("productKey") String productKey,
                                 @Param("deviceKey") String deviceKey);
}

