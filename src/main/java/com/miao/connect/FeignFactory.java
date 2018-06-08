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
 * @author Ryan
 */
public class FeignFactory {

    private static final Map<String, Connector> CONNECTORS = new ConcurrentHashMap<>();
    private final GsonEncoder gsonEncoder = new GsonEncoder();
    private final GsonDecoder gsonDecoder = new GsonDecoder();
    private final MyJacksonDecoder jacksonDecoder = new MyJacksonDecoder();
    private final MyJacksonEncoder jacksonEncoder = new MyJacksonEncoder();
    private final StringDecoder stringDecoder = new StringDecoder();

    private Default noSslVerifyClient = new Default(null, (s, sslSession) -> true);

    private final Environment environment;
    private final HystrixConfigurationProperties hystrixConfigurationProperties;

    public FeignFactory(Environment environment,
        HystrixConfigurationProperties hystrixConfigurationProperties) {
        this.environment = environment;
        this.hystrixConfigurationProperties = hystrixConfigurationProperties;
    }

    private Map<String, Object> hystrixConf = new HashMap<>();

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

    /**
     * Connector的方法名
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
    private Encoder encoder = gsonEncoder;
    private Decoder decoder = gsonDecoder;
    private Retryer retryer = Retryer.NEVER_RETRY;
    private Level logLevel = Level.NONE;
    private ErrorDecoder errorDecoder = new FeignErrorDecoder();
    private String url;
    private Integer readTimeout;
    private Integer connectTimeout;

    public FeignFactory encoder(Encoder encoder) {
        this.encoder = encoder;
        return this;
    }

    public FeignFactory gsonEncoder() {
        this.encoder = gsonEncoder;
        return this;
    }

    public FeignFactory jacksonEncoder() {
        this.encoder = jacksonEncoder;
        return this;
    }

    public FeignFactory decoder(Decoder decoder) {
        this.decoder = decoder;
        return this;
    }

    public FeignFactory gsonDecoder() {
        this.decoder = gsonDecoder;
        return this;
    }

    public FeignFactory jacksonDecoder() {
        this.decoder = jacksonDecoder;
        return this;
    }

    public FeignFactory stringDecoder() {
        this.decoder = stringDecoder;
        return this;
    }

    public FeignFactory retryer(Retryer retryer) {
        this.retryer = retryer;
        return this;
    }

    public FeignFactory logLevel(Level logLevel) {
        this.logLevel = logLevel;
        return this;
    }

    public FeignFactory errorDecoder(ErrorDecoder errorDecoder) {
        this.errorDecoder = errorDecoder;
        return this;
    }

    public FeignFactory setterFactory(SetterFactory setterFactory) {
        this.setterFactory = setterFactory;
        return this;
    }

    /**
     * 单独设置commandKey配置。默认default。若设置后，必须在application.yml里添加对应配置，否则将抛出FeignConfigurationException。
     *
     * @param connectorMethod Connector接口中声明请求的方法名
     */
    public FeignFactory connectorMethod(String connectorMethod) {
        this.connectorMethod = connectorMethod;
        return this;
    }


    @SuppressWarnings("unchecked")
    private <T extends Connector> T getConnector(Class<T> connectorClass) {
        //determine configuration key
        String commandConfigKey = connectorClass.getSimpleName() + "." + this.connectorMethod;

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
        String readTimeoutKey = "feign.endpointConfig." + commandConfigKey + ".readTimeoutMillis";
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
