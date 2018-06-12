package com.miao.connect;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.RuntimeJsonMappingException;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.Response;
import feign.Util;
import feign.codec.Decoder;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.Collections;

/**
 * Jackson自定义，用来支持LocalDate.
 *
 * @author Ryan
 */
public class MyJacksonDecoder implements Decoder {

    private final ObjectMapper mapper;

    public MyJacksonDecoder() {
        this(Collections.emptyList());
    }

    /**
     * 构造器.
     */
    public MyJacksonDecoder(Iterable<Module> modules) {
        this.mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.setSerializationInclusion(Include.NON_NULL)
            .configure(SerializationFeature.INDENT_OUTPUT, true)
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    public MyJacksonDecoder(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public Object decode(Response response, Type type) throws IOException {
        if (response.status() == 404) {
            return Util.emptyValueOf(type);
        } else if (response.body() == null) {
            return null;
        } else {
            Reader reader = response.body().asReader();
            if (!((Reader) reader).markSupported()) {
                reader = new BufferedReader((Reader) reader, 1);
            }

            try {
                ((Reader) reader).mark(1);
                if (((Reader) reader).read() == -1) {
                    return null;
                } else {
                    ((Reader) reader).reset();
                    return this.mapper.readValue((Reader) reader, this.mapper.constructType(type));
                }
            } catch (RuntimeJsonMappingException var5) {
                if (var5.getCause() != null && var5.getCause() instanceof IOException) {
                    throw (IOException) IOException.class.cast(var5.getCause());
                } else {
                    throw var5;
                }
            }
        }
    }
}
