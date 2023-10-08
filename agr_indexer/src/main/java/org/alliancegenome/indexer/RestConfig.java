package org.alliancegenome.indexer;

import org.alliancegenome.core.config.ConfigHelper;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.ws.rs.HeaderParam;
import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.ClientConfig;
import si.mazi.rescu.serialization.jackson.JacksonObjectMapperFactory;

@Slf4j
public class RestConfig {

	public static ClientConfig config = new ClientConfig();

	static {
		JacksonObjectMapperFactory objectMapperFactory = new JacksonObjectMapperFactory() {
			@Override
			public ObjectMapper createObjectMapper() {
				// Curation Object Mapper
				ObjectMapper mapper = new ObjectMapper();
				mapper.registerModule(new JavaTimeModule());
				mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
				mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
				mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
				mapper.setSerializationInclusion(Include.NON_NULL);
				mapper.setSerializationInclusion(Include.NON_EMPTY);
				return mapper;
			}

			@Override
			public void configureObjectMapper(ObjectMapper mapper) {
			}
		};
		config.setJacksonObjectMapperFactory(objectMapperFactory);
		config.addDefaultParam(HeaderParam.class, "Content-Type", "application/json");
		if (ConfigHelper.getCurationApiToken() != null && ConfigHelper.getCurationApiToken().length() > 0) {
			config.addDefaultParam(HeaderParam.class, "Authorization", ConfigHelper.getCurationApiToken());
			log.info("Using Authorization token");
		}
		config.setHttpConnTimeout(300000);
		config.setHttpReadTimeout(300000);
	}

}
