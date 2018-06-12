package com.miao.connect;

import com.netflix.config.ConfigurationManager;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import feign.Client.Default;
import feign.Logger.Level;
import feign.Request;
import feign.RequestInterceptor;
import feign.Retryer;
import feign.codec.Decoder;
import feign.codec.Encoder;
import feign.codec.ErrorDecoder;
import feign.codec.StringDecoder;
import feign.gson.GsonDecoder;
import feign.gson.GsonEncoder;
import feign.hystrix.HystrixFeign;
import feign.hystrix.SetterFactory;
import feign.slf4j.Slf4jLogger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import javax.annotation.PostConstruct;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.env.Environment;

/**
 * Feign的制造工厂，会用读取配置文件，生成对应的Connector.
 *
 * @author Ryan
 */
public class FeignFactory {

    private static final Map<String, Connector> CONNECTORS = new ConcurrentHashMap<>();
    private static final GsonEncoder GSON_ENCODER = new GsonEncoder();
    private static final GsonDecoder GSON_DECODER = new GsonDecoder();
    private static final MyJacksonDecoder JACKSON_DECODER = new MyJacksonDecoder();
    private static final MyJacksonEncoder JACKSON_ENCODER = new MyJacksonEncoder();
    private static final StringDecoder STRING_DECODER = new StringDecoder();

    private static Default noSslVerifyClient = new Default(null, (ssl, sslSession) -> true);

    private final Environment environment;
    private final HystrixConfigurationProperties hystrixConfigurationProperties;

    public FeignFactory(Environment environment,
        HystrixConfigurationProperties hystrixConfigurationProperties) {
        this.environment = environment;
        this.hystrixConfigurationProperties = hystrixConfigurationProperties;
    }

    private Map<String, Object> hystrixConf = new HashMap<>();

    /**
     * 初始化hystrix配置，读取环境变量.
     */
    @PostConstruct
    public void initHystrixConfiguration() {
        read(null, hystrixConfigurationProperties.getHystrixConfig());
        for (Entry<String, Object> conf : hystrixConf.entrySet()) {
            ConfigurationManager.getConfigInstance().setProperty(conf.getKey(), conf.getValue());
        }

    }

    private void read(String prefix, Map<String, Object> map) {
        for (Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            String newPrefix = prefix == null ? key : prefix + "." + key;
            Object value = entry.getValue();
            if (value instanceof LinkedHashMap) {
                read(newPrefix, (LinkedHashMap) value);
            } else {
                hystrixConf.put(newPrefix, value);
            }

        }
    }

    public Builder builder() {
        return new Builder(environment);
    }

    public static class Builder {

        private final Environment environment;

        /**
         * 创建builder.
         */
        Builder(Environment environment) {
            this.environment = environment;
        }

        /**
         * Connector的方法名.
         */
        private String connectorMethod = "default";
        private List<RequestInterceptor> requestInterceptors = new ArrayList<>();
        private SetterFactory setterFactory = (target, method) -> {
            String groupKey = target.name();
            String hystrixCommandKey =
                method.getDeclaringClass().getSimpleName() + "#" + method.getName();

            return HystrixCommand.Setter
                .withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
                .andCommandKey(HystrixCommandKey.Factory.asKey(hystrixCommandKey))
                ;
        };
        private Encoder encoder = JACKSON_ENCODER;
        private Decoder decoder = JACKSON_DECODER;
        private Retryer retryer = Retryer.NEVER_RETRY;
        private Level logLevel = Level.NONE;
        private ErrorDecoder errorDecoder = new FeignErrorDecoder();
        private String url;
        private Integer readTimeout;
        private Integer connectTimeout;

        public Builder encoder(Encoder encoder) {
            this.encoder = encoder;
            return this;
        }

        public Builder gsonEncoder() {
            this.encoder = GSON_ENCODER;
            return this;
        }

        public Builder jacksonEncoder() {
            this.encoder = JACKSON_ENCODER;
            return this;
        }

        public Builder decoder(Decoder decoder) {
            this.decoder = decoder;
            return this;
        }

        public Builder gsonDecoder() {
            this.decoder = GSON_DECODER;
            return this;
        }

        public Builder jacksonDecoder() {
            this.decoder = JACKSON_DECODER;
            return this;
        }

        public Builder stringDecoder() {
            this.decoder = STRING_DECODER;
            return this;
        }

        public Builder retryer(Retryer retryer) {
            this.retryer = retryer;
            return this;
        }

        public Builder logLevel(Level logLevel) {
            this.logLevel = logLevel;
            return this;
        }

        public Builder errorDecoder(ErrorDecoder errorDecoder) {
            this.errorDecoder = errorDecoder;
            return this;
        }

        public Builder setterFactory(SetterFactory setterFactory) {
            this.setterFactory = setterFactory;
            return this;
        }

        /**
         * 单独设置commandKey配置。默认default。若设置后，必须在application.yml里添加对应配置，否则将抛出FeignConfigurationException。
         *
         * @param connectorMethod Connector接口中声明请求的方法名
         */
        public Builder connectorMethod(String connectorMethod) {
            this.connectorMethod = connectorMethod;
            return this;
        }

        public Builder requestInterceptor(RequestInterceptor interceptor) {
            requestInterceptors.add(interceptor);
            return this;
        }

        public Builder requestInterceptors(List<RequestInterceptor> interceptors) {
            requestInterceptors.addAll(interceptors);
            return this;
        }


        /**
         * 转换是安全的，获取最终的Connector. 会根据connectClass + connectorMethodName来缓存生成的Connector，减少对象创建.
         */
        @SuppressWarnings("unchecked")
        public <T extends Connector> T getConnector(Class<T> connectorClass) {
            //determine configuration key
            final String commandConfigKey =
                connectorClass.getSimpleName() + "." + this.connectorMethod;

            return (T) CONNECTORS.computeIfAbsent(commandConfigKey, k -> {
                this.determineFeignConfiguration(commandConfigKey);

                return HystrixFeign.builder()
                    .setterFactory(setterFactory)
                    .client(noSslVerifyClient)
                    .retryer(retryer)
                    .logger(new Slf4jLogger())
                    .logLevel(logLevel)
                    .errorDecoder(errorDecoder)
                    .options(new Request.Options(connectTimeout, readTimeout))
                    .requestInterceptors(requestInterceptors)
                    .encoder(encoder)
                    .decoder(decoder)
                    .target(connectorClass, url);
            });

        }

        private void determineFeignConfiguration(String commandConfigKey) {

            String urlKey = "feign.endpointConfig." + commandConfigKey + ".url";
            String readTimeoutKey =
                "feign.endpointConfig." + commandConfigKey + ".readTimeoutMillis";
            String connectTimeoutKey =
                "feign.endpointConfig." + commandConfigKey + ".connectTimeoutMillis";

            this.url = environment.getProperty(urlKey);
            String readTimeout = environment.getProperty(readTimeoutKey);
            String connectTimeout = environment.getProperty(connectTimeoutKey);

            if (StringUtils.isBlank(url)) {
                throw new FeignConfigurationException("没有配置：" + urlKey);
            }
            if (StringUtils.isBlank(readTimeout)) {
                throw new FeignConfigurationException("没有配置：" + readTimeoutKey);
            }
            if (StringUtils.isBlank(connectTimeout)) {
                throw new FeignConfigurationException("没有配置：" + connectTimeoutKey);
            }
            this.readTimeout = Integer.parseInt(readTimeout);
            this.connectTimeout = Integer.parseInt(connectTimeout);
        }
    }


}
