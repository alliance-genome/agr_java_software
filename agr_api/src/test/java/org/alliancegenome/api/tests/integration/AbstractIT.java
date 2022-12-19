package org.alliancegenome.api.tests.integration;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.neo4j.view.OrthologyModule;
import org.junit.Before;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AbstractIT {

	ObjectMapper mapper = new ObjectMapper();

	@Before
	public void before() {
		ConfigHelper.init();

		mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		mapper.registerModule(new OrthologyModule());
	}

}
