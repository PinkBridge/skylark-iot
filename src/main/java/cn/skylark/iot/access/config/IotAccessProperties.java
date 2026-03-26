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
