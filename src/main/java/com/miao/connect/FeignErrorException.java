package com.miao.connect;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 定义Feign回到Error的时候异常.
 * @author Ryan
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class FeignErrorException extends RuntimeException {

    private int status;
    private String message;

    public FeignErrorException(int status, String message) {
        this.status = status;
        this.message = message;
    }

}
