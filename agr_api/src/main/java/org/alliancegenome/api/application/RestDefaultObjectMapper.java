package org.alliancegenome.api.application;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import org.alliancegenome.core.config.ConfigHelper;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

@Provider
public class RestDefaultObjectMapper implements ContextResolver<ObjectMapper> {

    private final ObjectMapper mapper;

    public RestDefaultObjectMapper() {
        mapper = new ObjectMapper();
        mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        if (!ConfigHelper.isProduction())
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
//        mapper.getSerializerProvider().setNullValueSerializer(new NullSerializer());
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

}

/*
class NullSerializer extends JsonSerializer<Object> {

    @Override
    public void serialize(Object o, com.fasterxml.jackson.core.JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, com.fasterxml.jackson.core.JsonProcessingException {
        jsonGenerator.writeString("");
    }
}*/
