package cn.skylark.iot.mgmt.mapper;

import cn.skylark.iot.mgmt.model.entity.DeviceEventRecordEntity;
import cn.skylark.iot.mgmt.model.entity.DevicePropertyRecordEntity;
import cn.skylark.iot.mgmt.model.entity.DeviceServiceRecordEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeviceRecordMapper {
    int insertPropertyRecord(DevicePropertyRecordEntity entity);

    int insertEventRecord(DeviceEventRecordEntity entity);

    int insertServiceRecord(DeviceServiceRecordEntity entity);

    List<DevicePropertyRecordEntity> listPropertyRecords(@Param("productKey") String productKey,
                                                         @Param("deviceKey") String deviceKey,
                                                         @Param("offset") int offset,
                                                         @Param("limit") int limit);

    long countPropertyRecords(@Param("productKey") String productKey,
                              @Param("deviceKey") String deviceKey);

    List<DeviceEventRecordEntity> listEventRecords(@Param("productKey") String productKey,
                                                   @Param("deviceKey") String deviceKey,
                                                   @Param("offset") int offset,
                                                   @Param("limit") int limit);

    long countEventRecords(@Param("productKey") String productKey,
                           @Param("deviceKey") String deviceKey);

    List<DeviceServiceRecordEntity> listServiceRecords(@Param("productKey") String productKey,
                                                       @Param("deviceKey") String deviceKey,
                                                       @Param("offset") int offset,
                                                       @Param("limit") int limit);

    long countServiceRecords(@Param("productKey") String productKey,
                             @Param("deviceKey") String deviceKey);
}
