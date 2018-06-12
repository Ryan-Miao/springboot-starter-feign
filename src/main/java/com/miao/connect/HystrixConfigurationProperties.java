package com.miao.connect;

import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 对应hystrix的配置.
 * @author Ryan
 */
@Component
@ConfigurationProperties(prefix = "feign")
@Data
public class HystrixConfigurationProperties {

    private Map<String, Object> hystrixConfig;

}
