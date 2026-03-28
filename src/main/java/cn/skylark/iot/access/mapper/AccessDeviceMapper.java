package cn.skylark.iot.access.mapper;

import cn.skylark.iot.access.model.AccessDeviceRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AccessDeviceMapper {
    List<AccessDeviceRecord> findByDeviceName(@Param("deviceName") String deviceName);
}

