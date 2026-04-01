package cn.skylark.iot.mgmt.mapper;

import cn.skylark.iot.mgmt.model.entity.DeviceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeviceMapper {
    int insert(DeviceEntity entity);

    DeviceEntity findByPkAndName(@Param("productKey") String productKey,
                                 @Param("deviceName") String deviceName);

    List<DeviceEntity> listByProductKey(@Param("productKey") String productKey);

    int updateDisplayName(@Param("productKey") String productKey,
                          @Param("deviceName") String deviceName,
                          @Param("displayName") String displayName);

    int updateProfile(@Param("productKey") String productKey,
                      @Param("deviceName") String deviceName,
                      @Param("displayName") String displayName,
                      @Param("deviceType") String deviceType,
                      @Param("protocolType") String protocolType,
                      @Param("protocolVersion") String protocolVersion);

    int updateStatus(@Param("productKey") String productKey,
                     @Param("deviceName") String deviceName,
                     @Param("status") String status);

    int updateSecret(@Param("productKey") String productKey,
                     @Param("deviceName") String deviceName,
                     @Param("secret") String secret);

    int deleteByPkAndName(@Param("productKey") String productKey,
                          @Param("deviceName") String deviceName);

    int deleteByProductKey(@Param("productKey") String productKey);
}

