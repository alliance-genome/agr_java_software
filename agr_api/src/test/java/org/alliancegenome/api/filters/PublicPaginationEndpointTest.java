package org.alliancegenome.api.filters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.net.URISyntaxException;
import java.util.List;

import org.alliancegenome.api.controller.AlleleController;
import org.alliancegenome.api.controller.DiseaseController;
import org.alliancegenome.api.controller.ExpressionController;
import org.alliancegenome.api.controller.GeneController;
import org.alliancegenome.api.controller.LiteratureController;
import org.alliancegenome.api.controller.SearchController;
import org.jboss.resteasy.mock.MockDispatcherFactory;
import org.jboss.resteasy.mock.MockHttpRequest;
import org.jboss.resteasy.mock.MockHttpResponse;
import org.jboss.resteasy.spi.Dispatcher;
import org.junit.Before;
import org.junit.Test;

public class PublicPaginationEndpointTest {

	private Dispatcher dispatcher;

	@Before
	public void setUp() {
		dispatcher = MockDispatcherFactory.createDispatcher();
		dispatcher.getProviderFactory().registerProvider(PublicPaginationRequestFilter.class);
		dispatcher.getRegistry().addPerRequestResource(GeneController.class);
		dispatcher.getRegistry().addPerRequestResource(DiseaseController.class);
		dispatcher.getRegistry().addPerRequestResource(AlleleController.class);
		dispatcher.getRegistry().addPerRequestResource(ExpressionController.class);
		dispatcher.getRegistry().addPerRequestResource(LiteratureController.class);
		dispatcher.getRegistry().addPerRequestResource(SearchController.class);
	}

	@Test
	public void rejectsUnsupportedPageAndOffsetWindowsBeforeElasticsearch() throws Exception {
		List<String> paths = List.of(
			"/gene/MGI:1/alleles?limit=1000&page=151",
			"/gene/MGI:1/allele-viewer-ids?page=7501",
			"/search?q=gene&limit=1000&offset=150000"
		);

		for (String path : paths) {
			MockHttpResponse response = invoke(path);
			assertEquals(path, 400, response.getStatus());
			assertFalse(path, response.getContentAsString().contains("max_result_window"));
		}
	}

	@Test
	public void rejectsOversizedLimitAcrossEveryPaginatedControllerFamily() throws Exception {
		List<String> paths = List.of(
			"/gene/MGI:1/alleles?limit=1001",
			"/gene/MGI:1/allele-viewer-ids?limit=1001",
			"/disease/DOID:1/alleles?limit=1001",
			"/allele/MGI:1/variants?limit=1001",
			"/expression?limit=1001",
			"/reference/AGRKB:1/disease-annotations?limit=1001"
		);

		for (String path : paths) {
			MockHttpResponse response = invoke(path);
			assertEquals(path, 400, response.getStatus());
			assertFalse(path, response.getContentAsString().contains("max_result_window"));
		}
	}

	private MockHttpResponse invoke(String path) throws URISyntaxException {
		MockHttpResponse response = new MockHttpResponse();
		dispatcher.invoke(MockHttpRequest.get(path), response);
		return response;
	}
}
