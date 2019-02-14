package org.alliancegenome.api.application;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

@Provider
public class RestDefaultObjectMapper implements ContextResolver<ObjectMapper> {

    private final ObjectMapper mapper;

    public RestDefaultObjectMapper() {
        mapper = new ObjectMapper();
        mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
        mapper.getSerializerProvider().setNullValueSerializer(new NullSerializer());
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }

}

class NullSerializer extends JsonSerializer<Object> {

    @Override
    public void serialize(Object o, com.fasterxml.jackson.core.JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException, com.fasterxml.jackson.core.JsonProcessingException {
        jsonGenerator.writeString("");
    }
}