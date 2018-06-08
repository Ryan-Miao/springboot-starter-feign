package com.test.example;

import com.miao.connect.FeignFactory;
import com.miao.connect.HystrixConfigurationProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

/**
 * 使用示例.
 * @author Ryan
 */
@SpringBootApplication
public class ExampleApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExampleApplication.class, args);
    }

    @Autowired
    private Environment environment;

    @Bean
    public FeignFactory feignFactory() {
        return new FeignFactory(environment, hystrixConfigurationProperties());
    }

    @Bean
    public HystrixConfigurationProperties hystrixConfigurationProperties() {
        return new HystrixConfigurationProperties();
    }
}
