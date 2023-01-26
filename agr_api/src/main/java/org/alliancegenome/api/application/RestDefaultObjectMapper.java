package org.alliancegenome.api.application;

import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Provider;

import org.alliancegenome.api.json.APIBeanSerializerModifier;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Provider
public class RestDefaultObjectMapper implements ContextResolver<ObjectMapper> {

	private final ObjectMapper mapper;

	public RestDefaultObjectMapper() {
		//Log.error("Does this happen? 1");
		mapper = new ObjectMapper();
		mapper.registerModule(new Jdk8Module());
		mapper.registerModule(new JavaTimeModule());
		mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
		mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
		mapper.setSerializerFactory(mapper.getSerializerFactory().withSerializerModifier(new APIBeanSerializerModifier()));
	}

	@Override
	public ObjectMapper getContext(Class<?> type) {
		//Log.error("Does this happen? 2");
		return mapper;
	}
}