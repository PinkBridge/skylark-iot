package cn.skylark.iot.mgmt.mapper;

import cn.skylark.iot.mgmt.model.entity.DeviceConnectRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeviceConnectRecordMapper {
    int insert(DeviceConnectRecordEntity entity);

    List<DeviceConnectRecordEntity> list(@Param("productKey") String productKey,
                                        @Param("deviceKey") String deviceKey,
                                        @Param("offset") int offset,
                                        @Param("limit") int limit);

    long count(@Param("productKey") String productKey,
               @Param("deviceKey") String deviceKey);
}

