package cn.skylark.iot.access.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@Data
@ConfigurationProperties(prefix = "iot.access")
public class IotAccessProperties {

    /**
     * 设备认证配置（MVP：静态配置；后续替换为设备管理服务或数据库）。
     */
    private List<DeviceCredential> devices = new ArrayList<DeviceCredential>();

    /**
     * EMQX Management API 配置（用于下行 publish）。
     */
    private EmqxManagement emqx = new EmqxManagement();
    private Auth auth = new Auth();
    private Acl acl = new Acl();
    /** EMQX 规则/Webhook 上下线回调；请求头携带密钥 {@code X-Emqx-Webhook-Secret}。 */
    private Webhook webhook = new Webhook();

    @Data
    public static class Webhook {
        /**
         * false 时 {@code /api/access/emqx/webhook/session} 返回 404，便于未配置时不暴露端点。
         */
        private boolean enabled = false;
        /**
         * 与 EMQX HTTP 请求头中的密钥一致；为空且 enabled=true 时每次调用返回 503。
         */
        private String secret = "";
    }

    @Data
    public static class Auth {
        /**
         * true: /auth 使用数据库（iot_device）校验；false: 使用静态 devices 配置。
         */
        private boolean useDb = true;
    }

    @Data
    public static class Acl {
        /**
         * true: /acl 使用数据库策略（iot_acl_policy）判定；false: 使用静态 ACL 逻辑。
         */
        private boolean useDb = false;
    }

    @Data
    public static class EmqxManagement {
        /**
         * EMQX Dashboard / Management API 根地址。
         * docker compose 网络内默认可用：http://emqx:18083
         */
        private String baseUrl = "http://emqx:18083";

        /**
         * EMQX API Key（推荐使用 EMQX Dashboard 创建的 API Key）。
         * 作为 BasicAuth username。
         */
        private String apiKey = "";

        /**
         * EMQX API Secret（作为 BasicAuth password）。
         */
        private String apiSecret = "";
    }

    @Data
    public static class DeviceCredential {
        private String deviceId;
        private String username;
        private String password;
        private boolean enabled = true;
        private String publishPrefix = "up/";
        private String subscribePrefix = "down/";
    }
}
