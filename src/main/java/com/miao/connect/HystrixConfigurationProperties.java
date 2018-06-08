package com.miao.connect;

import java.util.Map;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @author Ryan
 */
@Component
@ConfigurationProperties(prefix = "feign")
@Data
public class HystrixConfigurationProperties {

    private Map<String, Object> hystrixConfig;

}
