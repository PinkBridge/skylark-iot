package cn.skylark.iot.access.service;

import cn.skylark.iot.access.config.IotAccessProperties;
import cn.skylark.iot.access.mapper.AccessDeviceMapper;
import cn.skylark.iot.access.mapper.AclPolicyMapper;
import cn.skylark.iot.access.model.AccessDeviceRecord;
import cn.skylark.iot.access.model.AclPolicyRecord;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
public class DeviceAccessAuthServiceImpl implements DeviceAccessAuthService {

    private final IotAccessProperties properties;
    private final AccessDeviceMapper accessDeviceMapper;
    private final AclPolicyMapper aclPolicyMapper;
    private final AclMatcher aclMatcher;

    public DeviceAccessAuthServiceImpl(IotAccessProperties properties,
                                       AccessDeviceMapper accessDeviceMapper,
                                       AclPolicyMapper aclPolicyMapper,
                                       AclMatcher aclMatcher) {
        this.properties = properties;
        this.accessDeviceMapper = accessDeviceMapper;
        this.aclPolicyMapper = aclPolicyMapper;
        this.aclMatcher = aclMatcher;
    }

    @Override
    public boolean authenticate(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return false;
        }
        if (properties.getAuth() != null && properties.getAuth().isUseDb()) {
            return authenticateByDb(username, password);
        }
        return authenticateByStatic(username, password);
    }

    private boolean authenticateByDb(String username, String password) {
        List<AccessDeviceRecord> candidates = accessDeviceMapper.findByDeviceName(username.trim());
        if (candidates == null || candidates.isEmpty()) {
            return false;
        }
        for (AccessDeviceRecord item : candidates) {
            if (item == null) {
                continue;
            }
            if (!"enabled".equalsIgnoreCase(safe(item.getStatus()))) {
                continue;
            }
            if (equalsTrimmed(item.getDeviceName(), username) && equalsTrimmed(item.getSecret(), password)) {
                return true;
            }
        }
        return false;
    }

    private boolean authenticateByStatic(String username, String password) {
        for (IotAccessProperties.DeviceCredential item : properties.getDevices()) {
            if (item == null || !item.isEnabled()) {
                continue;
            }
            if (equalsTrimmed(item.getUsername(), username) && equalsTrimmed(item.getPassword(), password)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean allowAcl(String username, String action, String topic) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(action) || !StringUtils.hasText(topic)) {
            return false;
        }
        if (properties.getAcl() != null && properties.getAcl().isUseDb()) {
            return allowAclByDb(username, action, topic);
        }
        return allowAclByStatic(username, action, topic);
    }

    private boolean allowAclByDb(String username, String action, String topic) {
        AccessDeviceRecord device = getEnabledDevice(username);
        if (device == null) {
            return false;
        }
        String normalizedAction = action.trim().toLowerCase(Locale.ROOT);
        List<AclPolicyRecord> policies = aclPolicyMapper.findCandidates(
                safe(device.getProductKey()),
                normalizedAction,
                username.trim());
        return aclMatcher.isAllowed(policies, topic.trim());
    }

    private boolean allowAclByStatic(String username, String action, String topic) {
        IotAccessProperties.DeviceCredential target = null;
        for (IotAccessProperties.DeviceCredential item : properties.getDevices()) {
            if (item == null || !item.isEnabled()) {
                continue;
            }
            if (equalsTrimmed(item.getUsername(), username)) {
                target = item;
                break;
            }
        }
        if (target == null) {
            return false;
        }
        String a = action.trim().toLowerCase(Locale.ROOT);
        String t = topic.trim();

        if (t.startsWith("/sys/")) {
            return allowAliSysTopic(username, a, t);
        }

        if ("publish".equals(a)) {
            return startsWithTopicPrefix(t, safe(target.getPublishPrefix()));
        }
        if ("subscribe".equals(a)) {
            return startsWithTopicPrefix(t, safe(target.getSubscribePrefix()));
        }
        return false;
    }

    private AccessDeviceRecord getEnabledDevice(String username) {
        List<AccessDeviceRecord> devices = accessDeviceMapper.findByDeviceName(username.trim());
        if (devices == null || devices.isEmpty()) {
            return null;
        }
        for (AccessDeviceRecord item : devices) {
            if (item == null) {
                continue;
            }
            if (!equalsTrimmed(item.getDeviceName(), username)) {
                continue;
            }
            if ("enabled".equalsIgnoreCase(safe(item.getStatus()))) {
                return item;
            }
        }
        return null;
    }

    private static boolean allowAliSysTopic(String username, String action, String topic) {
        String[] parts = topic.split("/");
        if (parts.length < 5) {
            return false;
        }
        String deviceName = parts[3] == null ? "" : parts[3].trim();
        if (!equalsTrimmed(deviceName, username)) {
            return false;
        }
        if (!"thing".equals(parts[4])) {
            return false;
        }

        return "publish".equals(action) || "subscribe".equals(action);
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }

    private static boolean equalsTrimmed(String a, String b) {
        return safe(a).equals(safe(b));
    }

    private static boolean startsWithTopicPrefix(String topic, String prefix) {
        if (!StringUtils.hasText(prefix)) {
            return false;
        }
        return topic.equals(prefix) || topic.startsWith(prefix);
    }
}

