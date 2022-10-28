package org.alliancegenome.indexer;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.json.APIBeanSerializerModifier;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.config.RestDefaultObjectMapper;
import si.mazi.rescu.ClientConfig;
import si.mazi.rescu.serialization.jackson.JacksonObjectMapperFactory;

import javax.ws.rs.HeaderParam;

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
		if (ConfigHelper.getCurationApiToken() != null) {
			config.addDefaultParam(HeaderParam.class, "Authorization", ConfigHelper.getCurationApiToken());
			log.info("Using Authorization token");
		}
	}

}
