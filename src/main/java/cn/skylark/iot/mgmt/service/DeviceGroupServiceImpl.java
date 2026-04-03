package cn.skylark.iot.mgmt.service;

import cn.skylark.iot.common.tenant.TenantContext;
import cn.skylark.iot.mgmt.mapper.DeviceGroupMapper;
import cn.skylark.iot.mgmt.mapper.DeviceGroupRelMapper;
import cn.skylark.iot.mgmt.mapper.DeviceMapper;
import cn.skylark.iot.mgmt.model.dto.AddDevicesToGroupRequest;
import cn.skylark.iot.mgmt.model.dto.CreateDeviceGroupRequest;
import cn.skylark.iot.mgmt.model.dto.DeviceGroupMemberCount;
import cn.skylark.iot.mgmt.model.dto.DeviceGroupMemberRequest;
import cn.skylark.iot.mgmt.model.dto.DeviceGroupPageQuery;
import cn.skylark.iot.mgmt.model.dto.DeviceGroupPageResponse;
import cn.skylark.iot.mgmt.model.dto.DeviceGroupResponse;
import cn.skylark.iot.mgmt.model.dto.DeviceResponse;
import cn.skylark.iot.mgmt.model.dto.UpdateDeviceGroupRequest;
import cn.skylark.iot.mgmt.model.entity.DeviceEntity;
import cn.skylark.iot.mgmt.model.entity.DeviceGroupEntity;
import cn.skylark.iot.mgmt.model.entity.DeviceGroupRelEntity;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DeviceGroupServiceImpl implements DeviceGroupService {
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final DeviceGroupMapper deviceGroupMapper;
    private final DeviceGroupRelMapper deviceGroupRelMapper;
    private final DeviceMapper deviceMapper;

    public DeviceGroupServiceImpl(DeviceGroupMapper deviceGroupMapper,
                                  DeviceGroupRelMapper deviceGroupRelMapper,
                                  DeviceMapper deviceMapper) {
        this.deviceGroupMapper = deviceGroupMapper;
        this.deviceGroupRelMapper = deviceGroupRelMapper;
        this.deviceMapper = deviceMapper;
    }

    @Override
    public DeviceGroupResponse create(CreateDeviceGroupRequest request) {
        String name = request.getName() == null ? "" : request.getName().trim();
        if (name.isEmpty()) {
            throw new MgmtException(HttpStatus.BAD_REQUEST, "name required");
        }
        DeviceGroupEntity entity = new DeviceGroupEntity();
        entity.setTenantId(TenantContext.getTenantId());
        entity.setGroupKey(generateGroupKey12());
        entity.setName(name);
        entity.setDescription(trimToNull(request.getDescription()));
        try {
            deviceGroupMapper.insert(entity);
        } catch (DuplicateKeyException e) {
            throw new MgmtException(HttpStatus.CONFLICT, "group name already exists");
        }
        return get(entity.getGroupKey());
    }

    @Override
    public DeviceGroupResponse get(String groupKey) {
        DeviceGroupEntity entity = deviceGroupMapper.findByGroupKey(groupKey);
        if (entity == null) {
            throw new MgmtException(HttpStatus.NOT_FOUND, "group not found");
        }
        long deviceCount = deviceGroupRelMapper.countByGroupKey(groupKey);
        return toResponse(entity, deviceCount);
    }

    @Override
    public DeviceGroupPageResponse list(DeviceGroupPageQuery query) {
        int pageNum = query.getPageNum() == null || query.getPageNum() < 1 ? 1 : query.getPageNum();
        int pageSize = query.getPageSize() == null || query.getPageSize() < 1 ? 10 : Math.min(query.getPageSize(), 100);
        int offset = (pageNum - 1) * pageSize;
        String groupKeyFilter = trimToNull(query.getGroupKey());
        String nameFilter = trimToNull(query.getName());
        List<DeviceGroupEntity> list = deviceGroupMapper.listPage(groupKeyFilter, nameFilter, offset, pageSize);
        long total = deviceGroupMapper.countPage(groupKeyFilter, nameFilter);

        List<String> keys = new ArrayList<String>();
        for (DeviceGroupEntity e : list) {
            keys.add(e.getGroupKey());
        }
        Map<String, Long> countByKey = new HashMap<String, Long>();
        if (!keys.isEmpty()) {
            for (DeviceGroupMemberCount row : deviceGroupRelMapper.countMembersByGroupKeys(keys)) {
                countByKey.put(row.getGroupKey(), row.getMemberCount());
            }
        }

        List<DeviceGroupResponse> records = new ArrayList<DeviceGroupResponse>();
        for (DeviceGroupEntity item : list) {
            records.add(toResponse(item, countByKey.getOrDefault(item.getGroupKey(), 0L)));
        }

        DeviceGroupPageResponse response = new DeviceGroupPageResponse();
        response.setRecords(records);
        response.setTotal(total);
        response.setPageNum(pageNum);
        response.setPageSize(pageSize);
        return response;
    }

    @Override
    public DeviceGroupResponse update(String groupKey, UpdateDeviceGroupRequest request) {
        String name = request.getName() == null ? "" : request.getName().trim();
        if (name.isEmpty()) {
            throw new MgmtException(HttpStatus.BAD_REQUEST, "name required");
        }
        DeviceGroupEntity entity = new DeviceGroupEntity();
        entity.setGroupKey(groupKey);
        entity.setName(name);
        entity.setDescription(trimToNull(request.getDescription()));
        try {
            if (deviceGroupMapper.updateByGroupKey(entity) == 0) {
                throw new MgmtException(HttpStatus.NOT_FOUND, "group not found");
            }
        } catch (DuplicateKeyException e) {
            throw new MgmtException(HttpStatus.CONFLICT, "group name already exists");
        }
        return get(groupKey);
    }

    @Override
    public void delete(String groupKey) {
        // delete relations first to avoid dangling members
        deviceGroupRelMapper.deleteByGroupKey(groupKey);
        if (deviceGroupMapper.deleteByGroupKey(groupKey) == 0) {
            throw new MgmtException(HttpStatus.NOT_FOUND, "group not found");
        }
    }

    @Override
    public void addDevices(String groupKey, AddDevicesToGroupRequest request) {
        // ensure group exists
        deviceGroupMapper.findByGroupKey(groupKey);
        if (deviceGroupMapper.findByGroupKey(groupKey) == null) {
            throw new MgmtException(HttpStatus.NOT_FOUND, "group not found");
        }
        List<DeviceGroupMemberRequest> devices = request.getDevices();
        if (devices == null || devices.isEmpty()) {
            throw new MgmtException(HttpStatus.BAD_REQUEST, "devices required");
        }
        for (DeviceGroupMemberRequest item : devices) {
            if (item == null) continue;
            // ensure device exists in tenant
            DeviceEntity device = deviceMapper.findByPkAndDeviceKey(item.getProductKey(), item.getDeviceKey());
            if (device == null) {
                throw new MgmtException(HttpStatus.NOT_FOUND, "device not found");
            }
            DeviceGroupRelEntity rel = new DeviceGroupRelEntity();
            rel.setTenantId(TenantContext.getTenantId());
            rel.setGroupKey(groupKey);
            rel.setProductKey(item.getProductKey().trim());
            rel.setDeviceKey(item.getDeviceKey().trim());
            try {
                deviceGroupRelMapper.insert(rel);
            } catch (DuplicateKeyException ignored) {
                // idempotent add
            }
        }
    }

    @Override
    public void removeDevice(String groupKey, String productKey, String deviceKey) {
        if (deviceGroupRelMapper.delete(groupKey, productKey, deviceKey) == 0) {
            throw new MgmtException(HttpStatus.NOT_FOUND, "member not found");
        }
    }

    @Override
    public List<DeviceResponse> listGroupDevices(String groupKey) {
        if (deviceGroupMapper.findByGroupKey(groupKey) == null) {
            throw new MgmtException(HttpStatus.NOT_FOUND, "group not found");
        }
        List<DeviceGroupRelEntity> rels = deviceGroupRelMapper.listByGroupKey(groupKey);
        List<DeviceResponse> result = new ArrayList<DeviceResponse>();
        for (DeviceGroupRelEntity rel : rels) {
            DeviceEntity device = deviceMapper.findByPkAndDeviceKey(rel.getProductKey(), rel.getDeviceKey());
            if (device == null) {
                continue;
            }
            result.add(toDeviceResponse(device));
        }
        return result;
    }

    private static DeviceGroupResponse toResponse(DeviceGroupEntity entity, long deviceCount) {
        DeviceGroupResponse resp = new DeviceGroupResponse();
        resp.setGroupKey(entity.getGroupKey());
        resp.setName(entity.getName());
        resp.setDescription(entity.getDescription());
        resp.setDeviceCount(deviceCount);
        resp.setCreatedAt(entity.getCreatedAt());
        resp.setUpdatedAt(entity.getUpdatedAt());
        return resp;
    }

    private static DeviceResponse toDeviceResponse(DeviceEntity entity) {
        DeviceResponse resp = new DeviceResponse();
        resp.setProductKey(entity.getProductKey());
        resp.setDeviceKey(entity.getDeviceKey());
        resp.setDeviceName(entity.getDeviceName());
        resp.setAddress(entity.getAddress());
        resp.setDeviceType(entity.getDeviceType());
        resp.setStatus(entity.getStatus());
        resp.setConnectStatus(entity.getConnectStatus());
        resp.setLastConnectedAt(entity.getLastConnectedAt());
        resp.setLastDisconnectedAt(entity.getLastDisconnectedAt());
        resp.setSecret(entity.getSecret());
        resp.setProtocolType(entity.getProtocolType());
        resp.setProtocolVersion(entity.getProtocolVersion());
        return resp;
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String v = s.trim();
        return v.isEmpty() ? null : v;
    }

    private static String generateGroupKey12() {
        // 12 hex chars = 6 bytes
        return randomHex(6);
    }

    private static String randomHex(int bytes) {
        byte[] buf = new byte[bytes];
        SECURE_RANDOM.nextBytes(buf);
        char[] out = new char[bytes * 2];
        final char[] HEX = "0123456789abcdef".toCharArray();
        for (int i = 0; i < bytes; i++) {
            int v = buf[i] & 0xFF;
            out[i * 2] = HEX[v >>> 4];
            out[i * 2 + 1] = HEX[v & 0x0F];
        }
        return new String(out).toLowerCase(Locale.ROOT);
    }
}

