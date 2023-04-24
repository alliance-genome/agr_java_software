package org.alliancegenome.api.tests.integration;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.alliancegenome.api.service.AlleleService;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.junit.Test;


public class TransgenicAlleleIT {

	private AlleleService alleleService = new AlleleService();

	@Test
	public void checkAlleleTransgeneInSpeciesEndpoint() {
		Pagination pagination = new Pagination(1, 10, null, null);
		// muIs61
		String geneID = "WB:WBTransgene00001048";
		JsonResultResponse<Allele> response = alleleService.getAllelesBySpecies("elegans", pagination);
		assertResponse(response, 10, 8400);

		pagination.addFieldFilter(FieldFilter.SYMBOL, "muIs61");
		response = alleleService.getAllelesBySpecies("elegans", pagination);
		assertResponse(response, 1, 1);
		assertEquals(response.getResults().get(0).getPrimaryKey(), "WB:WBTransgene00001048");
	}


	@Test
	public void filterTransgenicAllelesBySymbol() {
		Pagination pagination = new Pagination();
		JsonResultResponse<Allele> response = alleleService.getTransgenicAlleles("WB:WBGene00002992", pagination);
		int total = response.getTotal();
		assertThat(total, greaterThan(1));

		String firstAlleleSymbol = response.getResults().get(0).getSymbolText();
		pagination.addFieldFilter(FieldFilter.SYMBOL, firstAlleleSymbol);
		response = alleleService.getTransgenicAlleles("WB:WBGene00002992", pagination);
		assertEquals(response.getTotal(), 1);
	}

	@Test
	public void filterTransgenicAllelesByConstruct() {
		Pagination pagination = new Pagination();
		JsonResultResponse<Allele> response = alleleService.getTransgenicAlleles("WB:WBGene00002992", pagination);
		int total = response.getTotal();
		assertThat(total, greaterThan(1));

		String firstConstructSymbol = response.getResults().get(0).getConstructs().get(0).getNameText();
		pagination.addFieldFilter(FieldFilter.CONSTRUCT_SYMBOL, firstConstructSymbol);
		response = alleleService.getTransgenicAlleles("WB:WBGene00002992", pagination);
		assertEquals(response.getTotal(), 1);
	}

	@Test
	public void filterTransgenicAllelesBySynonym() {
		Pagination pagination = new Pagination();
		JsonResultResponse<Allele> response = alleleService.getTransgenicAlleles("WB:WBGene00002992", pagination);
		int total = response.getTotal();
		assertThat(total, greaterThan(1));

		pagination.addFieldFilter(FieldFilter.SYNONYMS, "dsred");
		response = alleleService.getTransgenicAlleles("WB:WBGene00002992", pagination);
		assertEquals(response.getTotal(), 1);
	}

	@Test
	public void filterTransgenicAllelesByPhenotype() {
		Pagination pagination = new Pagination();
		JsonResultResponse<Allele> response = alleleService.getTransgenicAlleles("WB:WBGene00002992", pagination);
		int total = response.getTotal();
		assertThat(total, greaterThan(1));

		pagination.addFieldFilter(FieldFilter.TRANSGENE_HAS_PHENOTYPE, "true");
		response = alleleService.getTransgenicAlleles("WB:WBGene00002992", pagination);
		assertThat(total, greaterThan(1));
	}

	@Test
	public void filterTransgenicAllelesByDisease() {
		Pagination pagination = new Pagination();
		final String geneID = "FB:FBgn0284084";
		JsonResultResponse<Allele> response = alleleService.getTransgenicAlleles(geneID, pagination);
		int total = response.getTotal();
		assertThat(total, greaterThan(100));

		pagination.addFieldFilter(FieldFilter.TRANSGENE_HAS_DISEASE, "true");
		response = alleleService.getTransgenicAlleles(geneID, pagination);
		assertThat(response.getTotal(), greaterThan(1));
		assertThat(response.getTotal(), lessThan(5));
	}

	@Test
	public void filterTransgenicAllelesByRegulatedGenes() {
		Pagination pagination = new Pagination();
		String geneID = "FB:FBgn0261532";
		JsonResultResponse<Allele> response = alleleService.getTransgenicAlleles(geneID, pagination);
		int total = response.getTotal();
		assertThat(total, greaterThan(1));

		pagination.addFieldFilter(FieldFilter.CONSTRUCT_REGULATED_GENE, "uast");
		response = alleleService.getTransgenicAlleles(geneID, pagination);
		assertThat(response.getTotal(), greaterThanOrEqualTo(4));
	}

	@Test
	public void filterTransgenicAllelesByTargetedGenes() {
		Pagination pagination = new Pagination();
		String geneID = "FB:FBgn0261532";
		JsonResultResponse<Allele> response = alleleService.getTransgenicAlleles(geneID, pagination);
		int total = response.getTotal();
		assertThat(total, greaterThan(8));

		pagination.addFieldFilter(FieldFilter.CONSTRUCT_TARGETED_GENE, "cdm");
		response = alleleService.getTransgenicAlleles(geneID, pagination);
		assertThat(response.getTotal(), greaterThanOrEqualTo(4));
	}

	@Test
	public void filterTransgenicAllelesByConstructNonBGI() {
		Pagination pagination = new Pagination();
		JsonResultResponse<Allele> response = alleleService.getTransgenicAlleles("HGNC:12779", pagination);
		int total = response.getTotal();
		assertEquals(total, 1);

		pagination.addFieldFilter(FieldFilter.CONSTRUCT_SYMBOL, "no-match");
		response = alleleService.getTransgenicAlleles("WB:WBGene00002992", pagination);
		assertEquals(response.getTotal(), 0);
	}

	private void assertResponse(JsonResultResponse<Allele> response, int resultSize, int totalSize) {
		assertNotNull(response);
		assertThat("Number of returned records", response.getResults().size(), greaterThanOrEqualTo(resultSize));
		assertThat("Number of total records", response.getTotal(), greaterThanOrEqualTo(totalSize));
	}

}
