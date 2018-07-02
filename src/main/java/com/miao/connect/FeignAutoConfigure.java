package com.miao.connect;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

/**
 * 自动配置类.
 *
 * @author Ryan Miao at 2018-07-02 10:47
 **/
@Configuration
@ConditionalOnClass(FeignFactory.class)
@EnableConfigurationProperties(HystrixConfigurationProperties.class)
public class FeignAutoConfigure {

    @Autowired
    private Environment environment;
    @Autowired
    private HystrixConfigurationProperties hystrixConfigurationProperties;

    @Bean
    @ConditionalOnMissingBean
    public FeignFactory feignFactory() {
        return new FeignFactory(environment, hystrixConfigurationProperties);
    }

}
