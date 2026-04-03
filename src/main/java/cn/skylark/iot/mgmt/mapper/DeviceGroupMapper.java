package cn.skylark.iot.mgmt.mapper;

import cn.skylark.iot.mgmt.model.entity.DeviceGroupEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DeviceGroupMapper {
    int insert(DeviceGroupEntity entity);

    DeviceGroupEntity findByGroupKey(@Param("groupKey") String groupKey);

    List<DeviceGroupEntity> listPage(@Param("groupKey") String groupKey,
                                     @Param("name") String name,
                                     @Param("offset") int offset,
                                     @Param("pageSize") int pageSize);

    long countPage(@Param("groupKey") String groupKey, @Param("name") String name);

    int updateByGroupKey(DeviceGroupEntity entity);

    int deleteByGroupKey(@Param("groupKey") String groupKey);
}

