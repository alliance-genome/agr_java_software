package org.alliancegenome.api.application;

import javax.ws.rs.ext.*;

import org.alliancegenome.api.json.APIBeanSerializerModifier;
import org.alliancegenome.core.config.ConfigHelper;

import com.fasterxml.jackson.databind.*;

@Provider
public class RestDefaultObjectMapper implements ContextResolver<ObjectMapper> {

    private final ObjectMapper mapper;

    public RestDefaultObjectMapper() {
        mapper = new ObjectMapper();
        mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        if (!ConfigHelper.isProduction())
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.setSerializerFactory(mapper.getSerializerFactory().withSerializerModifier(new APIBeanSerializerModifier()));
    }

    @Override
    public ObjectMapper getContext(Class<?> type) {
        return mapper;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }
}