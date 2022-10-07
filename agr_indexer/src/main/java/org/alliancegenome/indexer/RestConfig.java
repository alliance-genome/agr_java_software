package org.alliancegenome.indexer;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.alliancegenome.api.json.APIBeanSerializerModifier;
import org.alliancegenome.core.config.ConfigHelper;
import si.mazi.rescu.ClientConfig;
import si.mazi.rescu.serialization.jackson.DefaultJacksonObjectMapperFactory;
import si.mazi.rescu.serialization.jackson.JacksonObjectMapperFactory;

import javax.ws.rs.HeaderParam;

public class RestConfig {

    public static ClientConfig config = new ClientConfig();

    static {
        JacksonObjectMapperFactory objectMapperFactory = new JacksonObjectMapperFactory() {
            @Override
            public ObjectMapper createObjectMapper() {
                ObjectMapper mapper = new ObjectMapper();
                mapper.registerModule(new JavaTimeModule());
                mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
                mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
                if (!ConfigHelper.isProduction())
                    mapper.enable(SerializationFeature.INDENT_OUTPUT);
                mapper.setSerializerFactory(mapper.getSerializerFactory().withSerializerModifier(new APIBeanSerializerModifier()));
                return mapper;
            }

            @Override
            public void configureObjectMapper(ObjectMapper mapper) {
            }
        };
        config.setJacksonObjectMapperFactory(objectMapperFactory);
        config.addDefaultParam(HeaderParam.class, "Content-Type", "application/json");
    }

}
