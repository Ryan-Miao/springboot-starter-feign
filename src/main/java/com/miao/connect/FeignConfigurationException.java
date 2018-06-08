package com.miao.connect;

/**
 * 当Feign的url，timeout没有配置的时候，抛出此异常。
 * @author Ryan Miao at 2018-06-08 18:20
 **/
public class FeignConfigurationException extends RuntimeException {

    public FeignConfigurationException(String message) {
        super(message);
    }
}
