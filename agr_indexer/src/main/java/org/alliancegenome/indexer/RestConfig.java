package org.alliancegenome.indexer;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.config.RestDefaultObjectMapper;

import com.fasterxml.jackson.databind.ObjectMapper;

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
				RestDefaultObjectMapper mappedProducer = new RestDefaultObjectMapper();
				return mappedProducer.getMapper();
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
