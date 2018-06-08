package com.miao.connect;

import feign.Response;
import feign.Response.Body;
import feign.codec.ErrorDecoder;
import java.io.IOException;
import org.apache.commons.io.IOUtils;

/**
 * @author Ryan
 */
public class FeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();
        String reason = response.reason();
        String bodyMsg = "";
        Body body = response.body();
        if (body != null) {
            try {
                bodyMsg = IOUtils.toString(body.asReader());
            } catch (IOException ignore) {
            }
        }

        return new FeignErrorException(status, bodyMsg);
    }
}
