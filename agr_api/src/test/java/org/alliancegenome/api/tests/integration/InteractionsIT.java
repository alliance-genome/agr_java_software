package org.alliancegenome.api.tests.integration;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Map;

import org.alliancegenome.api.entity.GeneGeneticInteractionDocument;
import org.alliancegenome.api.entity.GeneMolecularInteractionDocument;
import org.alliancegenome.api.service.GeneService;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.repository.InteractionRepository;
import org.junit.Test;

import jakarta.inject.Inject;

public class InteractionsIT {

	public static InteractionRepository repo = new InteractionRepository();
	
	@Inject
	private GeneService geneService;

	public static void main(String[] args) throws Exception {

	}

	@Test
	public void getGeneticInteractionFieldValues() {
		JsonResultResponse<GeneGeneticInteractionDocument> response = geneService.getGeneticInteractions("MGI:109583", new Pagination());
		assertNotNull(response);

		final Map<String, List<String>> distinctFieldValues = response.retrieveDistinctFieldValues();
		assertNotNull(distinctFieldValues);
		assertThat(3, greaterThanOrEqualTo(distinctFieldValues.size()));

	}

	@Test
	public void getMolecularInteractionFieldValues() {
		JsonResultResponse<GeneMolecularInteractionDocument> response = geneService.getMolecularInteractions("MGI:109583", new Pagination());
		assertNotNull(response);

		final Map<String, List<String>> distinctFieldValues = response.retrieveDistinctFieldValues();
		assertNotNull(distinctFieldValues);
		assertThat(3, greaterThanOrEqualTo(distinctFieldValues.size()));

	}
}
