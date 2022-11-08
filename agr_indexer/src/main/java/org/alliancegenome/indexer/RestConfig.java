package org.alliancegenome.indexer;

import javax.ws.rs.HeaderParam;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.config.RestDefaultObjectMapper;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;
import si.mazi.rescu.ClientConfig;
import si.mazi.rescu.serialization.jackson.JacksonObjectMapperFactory;

@Log4j2
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
