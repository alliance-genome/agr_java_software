package org.alliancegenome.es.rest;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.entities.ResourceDescriptor;
import org.alliancegenome.curation_api.model.entities.ResourceDescriptorPage;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.ws.rs.HeaderParam;
import lombok.extern.slf4j.Slf4j;
import si.mazi.rescu.ClientConfig;
import si.mazi.rescu.serialization.jackson.JacksonObjectMapperFactory;

@Slf4j
public class RestConfig {

	private RestConfig() {
	}

	public static ClientConfig config = new ClientConfig();

	static {
		JacksonObjectMapperFactory objectMapperFactory = new JacksonObjectMapperFactory() {
			@Override
			public ObjectMapper createObjectMapper() {
				return RestConfig.createObjectMapper();
			}

			@Override
			public void configureObjectMapper(ObjectMapper mapper) {
			}
		};
		config.setJacksonObjectMapperFactory(objectMapperFactory);
		config.addDefaultParam(HeaderParam.class, "Content-Type", "application/json");
		if (ConfigHelper.getCurationApiToken() != null && !ConfigHelper.getCurationApiToken().isEmpty()) {
			config.addDefaultParam(HeaderParam.class, "Authorization", ConfigHelper.getCurationApiToken());
			log.info("Using Authorization token");
		}
		config.setHttpConnTimeout(600000);
		config.setHttpReadTimeout(600000);
	}

	public static ObjectMapper createObjectMapper() {
		// Curation Object Mapper
		JsonFactory factory = JsonFactory.builder()
			.enable(StreamReadFeature.INCLUDE_SOURCE_IN_LOCATION)
			.build();

		ObjectMapper mapper = new ObjectMapper(factory);
		mapper.registerModule(new JavaTimeModule());
		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
		mapper.setSerializationInclusion(Include.NON_NULL);
		mapper.setSerializationInclusion(Include.NON_EMPTY);
		mapper.registerSubtypes(
			new NamedType(ResourceDescriptor.class, "ResourceDescriptor"),
			new NamedType(ResourceDescriptorPage.class, "ResourceDescriptorPage")
		);
		return mapper;
	}

}
