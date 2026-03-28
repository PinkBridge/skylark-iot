package cn.skylark.iot.access.service;

import cn.skylark.iot.access.model.AclPolicyRecord;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Component
public class AclMatcher {

    public boolean isAllowed(List<AclPolicyRecord> candidates, String topic) {
        if (candidates == null || candidates.isEmpty() || !StringUtils.hasText(topic)) {
            return false;
        }
        for (AclPolicyRecord policy : candidates) {
            if (policy == null || !Boolean.TRUE.equals(policy.getEnabled())) {
                continue;
            }
            if (!matchTopic(policy.getTopicPattern(), topic)) {
                continue;
            }
            String effect = safe(policy.getEffect()).toLowerCase(Locale.ROOT);
            if ("deny".equals(effect)) {
                return false;
            }
            if ("allow".equals(effect)) {
                return true;
            }
        }
        return false;
    }

    /**
     * MQTT topic wildcard match:
     * + : single level
     * # : multi level (must be last token)
     */
    static boolean matchTopic(String pattern, String topic) {
        if (!StringUtils.hasText(pattern) || !StringUtils.hasText(topic)) {
            return false;
        }
        String[] p = pattern.split("/");
        String[] t = topic.split("/");
        int i = 0;
        int j = 0;
        while (i < p.length && j < t.length) {
            if ("#".equals(p[i])) {
                return i == p.length - 1;
            }
            if ("+".equals(p[i])) {
                i++;
                j++;
                continue;
            }
            if (!p[i].equals(t[j])) {
                return false;
            }
            i++;
            j++;
        }
        if (i == p.length && j == t.length) {
            return true;
        }
        return i == p.length - 1 && "#".equals(p[i]);
    }

    private static String safe(String s) {
        return s == null ? "" : s.trim();
    }
}

