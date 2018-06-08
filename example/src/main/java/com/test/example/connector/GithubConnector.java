package com.test.example.connector;

import com.miao.connect.Connector;
import com.test.example.connector.contract.GithubUser;
import feign.Headers;
import feign.Param;
import feign.RequestLine;
import rx.Observable;

/**
 * @author Ryan Miao at 2018-06-08 15:15
 **/
public interface GithubConnector extends Connector {

    @RequestLine("GET /users/{username}")
    @Headers({"Content-Type: application/json"})
    GithubUser getGithubUser(@Param("username") String username);

    @RequestLine("GET /users/{username}/repos")
    @Headers({"Content-Type: application/json"})
    Observable<String> getRepos(@Param("username") String username);
}
