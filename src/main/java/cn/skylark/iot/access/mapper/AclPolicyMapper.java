package cn.skylark.iot.access.mapper;

import cn.skylark.iot.access.model.AclPolicyRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AclPolicyMapper {
    int insert(AclPolicyRecord record);

    List<AclPolicyRecord> findProductTemplates(@Param("productKey") String productKey);

    List<AclPolicyRecord> listProductChannels(@Param("productKey") String productKey);

    List<AclPolicyRecord> listDeviceChannels(@Param("productKey") String productKey,
                                             @Param("deviceKey") String deviceKey);

    int updateEffectAndEnabledById(@Param("id") Long id,
                                   @Param("productKey") String productKey,
                                   @Param("effect") String effect,
                                   @Param("enabled") Boolean enabled);

    int updateDeviceEffectAndEnabledById(@Param("id") Long id,
                                         @Param("productKey") String productKey,
                                         @Param("deviceKey") String deviceKey,
                                         @Param("effect") String effect,
                                         @Param("enabled") Boolean enabled);

    int replaceDevicePlaceholders(@Param("productKey") String productKey,
                                  @Param("deviceKey") String deviceKey);

    List<AclPolicyRecord> findCandidates(@Param("productKey") String productKey,
                                         @Param("action") String action,
                                         @Param("subjectValue") String subjectValue);
}

