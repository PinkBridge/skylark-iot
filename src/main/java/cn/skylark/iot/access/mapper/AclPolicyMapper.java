package cn.skylark.iot.access.mapper;

import cn.skylark.iot.access.model.AclPolicyRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AclPolicyMapper {
    List<AclPolicyRecord> findCandidates(@Param("productKey") String productKey,
                                         @Param("action") String action,
                                         @Param("subjectValue") String subjectValue);
}

