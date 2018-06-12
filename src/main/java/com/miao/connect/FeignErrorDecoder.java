package com.miao.connect;

import feign.Response;
import feign.Response.Body;
import feign.codec.ErrorDecoder;
import java.io.IOException;
import org.apache.commons.io.IOUtils;

/**
 * Feign请求发生错误的时候处理方案，这里仅仅把错误的body返回，放在FeignErrorException的message里。 feign会把大于200的结果都当做错误.
 *
 * @author Ryan
 */
public class FeignErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String methodKey, Response response) {
        int status = response.status();
        String bodyMsg = response.reason();
        Body body = response.body();
        if (body != null) {
            try {
                bodyMsg = IOUtils.toString(body.asReader());
            } catch (IOException ignore) {
                //ignore
            }
        }

        return new FeignErrorException(status, bodyMsg);
    }
}
