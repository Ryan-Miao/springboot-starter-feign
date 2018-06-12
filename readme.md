Feign集成工具
====

![](https://img.shields.io/badge/Java-1.8-orange.svg)
[![](https://img.shields.io/badge/OpenFeign-9.7.0-green.svg)](https://github.com/OpenFeign/feign/tree/master/hystrix)
![](https://img.shields.io/badge/Springboot-1.5+-green.svg)

[![Build Status](https://travis-ci.org/Ryan-Miao/springboot-starter-feign.svg?branch=master)](https://travis-ci.org/Ryan-Miao/springboot-starter-feign)


如果不使用SpringCloud，单独使用OpenFeign怎么用？

本项目提供了一个开箱即用的spring boot feign starter, 基于默认的约定配置
来简化和优化OpenFeign的使用流程.



## How to use

引入repo

```xml
<repositories>
    <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
    </repository>
</repositories>
```


引入依赖
```xml
<dependency>
    <groupId>com.github.Ryan-Miao</groupId>
    <artifactId>springboot-starter-feign</artifactId>
    <version>1.1</version>
</dependency>
```


在springboot 项目中添加Configuration

```java
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
```


然后就可以使用了。



### 使用和配置
约定了一些配置，大概如下

```yml
feign:
  hystrixConfig:
    "hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds": 8000
    "hystrix.command.GithubConnector#getRepos.execution.isolation.thread.timeoutInMilliseconds": 15000
  endpointConfig:
    GithubConnector:
      default:
        url: https://api.github.com
        readTimeoutMillis: 8000
        connectTimeoutMillis: 5000
      getRepos:
        url: https://api.github.com
        readTimeoutMillis: 15000
        connectTimeoutMillis: 10000
```

- feign是配置的第一个索引
- hystrixConfig是hystrix的配置，更多配置见[Hystrix](https://github.com/Netflix/Hystrix/)
- endpointConfig是我们远程请求的host和超时配置，其中，第一个节点为Connector class
的名称，下一个是具体到某个请求的key，整个Connector class的默认配置是default
节点，如果该Connector里的某个请求的超时比较长，需要单独设置，则会覆盖默认节点。
另外，hystrix的超时配置commankey为[connectorClassName][#][methodName]



定义一个GithubConnector，继承`com.miao.connect.Connector`

```java
public interface GithubConnector extends Connector {

    @RequestLine("GET /users/{username}")
    @Headers({"Content-Type: application/json"})
    GithubUser getGithubUser(@Param("username") String username);

    @RequestLine("GET /users/{username}/repos")
    @Headers({"Content-Type: application/json"})
    Observable<String> getRepos(@Param("username") String username);
}
```


调用

```java
@Autowired
private FeignFactory feignFactory;


@GetMapping("/profile/{username}")
public GithubUser getProfile(@PathVariable String username) {
    //采用Jackson作为编码和解码类库，url和超时配置按照default，即读取feign.endpointConfig.GithubConnector.default
    final GithubConnector connector = feignFactory.builder().getConnector(GithubConnector.class);
    return connector.getGithubUser(username);
}

@GetMapping("/repos/{username}")
public String getUserRepos(@PathVariable String username) {
    //用String来接收返回值， url和超时单独指定配置，因为请求时间较长
    //采用connector的method来当做获取配置的key，即读取feign.endpointConfig.GithubConnector.getRepos
    final GithubConnector connector = feignFactory.builder()
        .connectorMethod("getRepos")
        .stringDecoder()  //默认使用jackson作为序列化工具，这里接收string，使用StringDecoder
        .getConnector(GithubConnector.class);
    return connector.getRepos(username)
        .onErrorReturn(e -> {
            LOGGER.error("请求出错", e);
            Throwable cause = e.getCause();
            if (cause instanceof FeignErrorException) {
                throw (FeignErrorException) cause;
            }
            throw new RuntimeException("请求失败", e);
        }).toBlocking().first();
}
```

具体见[使用示例example](example)



## 相比原生有什么区别？

最大的区别是hystrix配置的内容，原生并没有提供hystrix相关配置，需要自己额外
准备。这里集成hystrix的约定，只要按照hystrix官方参数配置即可。

然后是缓存，在使用原生OpenFeign的过程中发现每次请求都要创建一个Connector,
而且Connector的创建又依赖一大堆别的class。对于我们远程调用比较频繁的应用来说，
增大了垃圾收集器的开销，我们其实不想回收。所以对Connector做了缓存。

其他用法同OpenFeign。
