package cn.skylark.iot.mgmt.mapper;

import cn.skylark.iot.mgmt.model.entity.DeviceEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeviceMapper {
    int insert(DeviceEntity entity);

    DeviceEntity findByPkAndDeviceKey(@Param("productKey") String productKey,
                                      @Param("deviceKey") String deviceKey);

    List<DeviceEntity> listByProductKey(@Param("productKey") String productKey);

    List<DeviceEntity> listAll();

    List<DeviceEntity> listAllPage(@Param("keyword") String keyword,
                                   @Param("offset") int offset,
                                   @Param("pageSize") int pageSize);

    long countAllPage(@Param("keyword") String keyword);

    long countByProductKey(@Param("productKey") String productKey);

    int updateProfile(@Param("productKey") String productKey,
                      @Param("deviceKey") String deviceKey,
                      @Param("deviceType") String deviceType,
                      @Param("protocolType") String protocolType,
                      @Param("protocolVersion") String protocolVersion);

    int updateName(@Param("productKey") String productKey,
                   @Param("deviceKey") String deviceKey,
                   @Param("deviceName") String deviceName,
                   @Param("address") String address);

    int updateStatus(@Param("productKey") String productKey,
                     @Param("deviceKey") String deviceKey,
                     @Param("status") String status);

    int updateConnectStatus(@Param("productKey") String productKey,
                            @Param("deviceKey") String deviceKey,
                            @Param("connectStatus") String connectStatus);

    int updateConnectStatusWithLastTime(@Param("productKey") String productKey,
                                        @Param("deviceKey") String deviceKey,
                                        @Param("connectStatus") String connectStatus);

    int updateSecret(@Param("productKey") String productKey,
                     @Param("deviceKey") String deviceKey,
                     @Param("secret") String secret);

    int deleteByPkAndDeviceKey(@Param("productKey") String productKey,
                               @Param("deviceKey") String deviceKey);

    int deleteByProductKey(@Param("productKey") String productKey);
}
