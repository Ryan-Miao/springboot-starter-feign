package com.test.example.controller;

import com.miao.connect.FeignErrorException;
import com.miao.connect.FeignFactory;
import com.test.example.connector.GithubConnector;
import com.test.example.connector.contract.GithubUser;
import io.swagger.annotations.Api;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Ryan Miao at 2018-06-08 15:11
 **/
@Api
@RestController
@RequestMapping("/api/v1/github")
public class GithubController {

    private static final Logger LOGGER = LoggerFactory.getLogger(GithubController.class);

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


}
