package cn.skylark.iot.access;

import cn.skylark.iot.access.config.IotAccessProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
@EnableConfigurationProperties(IotAccessProperties.class)
public class IotAccessApplication {

    public static void main(String[] args) {
        SpringApplication.run(IotAccessApplication.class, args);
    }
}
