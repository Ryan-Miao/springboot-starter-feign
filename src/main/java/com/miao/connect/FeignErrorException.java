package com.miao.connect;

import lombok.Data;

/**
 * @author Ryan
 */
@Data
public class FeignErrorException extends RuntimeException {

    private int status;
    private String message;

    public FeignErrorException(int status, String message) {
        this.status = status;
        this.message = message;
    }

}
