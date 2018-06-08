package com.miao.connect;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.RequestTemplate;
import feign.codec.EncodeException;
import feign.codec.Encoder;
import java.lang.reflect.Type;
import java.util.Collections;

public class MyJacksonEncoder implements Encoder {

    private final ObjectMapper mapper;

    public MyJacksonEncoder() {
        this(Collections.emptyList());
    }

    public MyJacksonEncoder(Iterable<Module> modules) {
        this.mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setSerializationInclusion(Include.NON_NULL)
            .configure(SerializationFeature.INDENT_OUTPUT, true);
    }

    public MyJacksonEncoder(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public void encode(Object object, Type bodyType, RequestTemplate template) {
        try {
            JavaType javaType = this.mapper.getTypeFactory().constructType(bodyType);
            String bodyText = this.mapper.writerFor(javaType).writeValueAsString(object);
            template.body(bodyText);
        } catch (JsonProcessingException var5) {
            throw new EncodeException(var5.getMessage(), var5);
        }
    }
}