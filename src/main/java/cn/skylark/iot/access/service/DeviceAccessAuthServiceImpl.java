package cn.skylark.iot.access.service;

import cn.skylark.iot.access.config.IotAccessProperties;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Locale;

@Service
public class DeviceAccessAuthServiceImpl implements DeviceAccessAuthService {

    private final IotAccessProperties properties;

    public DeviceAccessAuthServiceImpl(IotAccessProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean authenticate(String username, String password) {
        if (!StringUtils.hasText(username) || !StringUtils.hasText(password)) {
            return false;
        }
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

