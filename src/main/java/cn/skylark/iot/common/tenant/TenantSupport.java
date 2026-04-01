package cn.skylark.iot.common.tenant;

import cn.skylark.iot.mgmt.service.MgmtException;
import org.springframework.http.HttpStatus;

public final class TenantSupport {
    private TenantSupport() {
    }

    public static Long requireTenantId() {
        Long tenantId = TenantContext.getTenantId();
        if (tenantId == null) {
            throw new MgmtException(HttpStatus.BAD_REQUEST, "tenant id is required");
        }
        return tenantId;
    }
}
