package org.alliancegenome.api.tests.integration;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.inject.Inject;

import org.alliancegenome.api.dto.EntitySubgroupSlim;
import org.alliancegenome.api.dto.RibbonSummary;
import org.alliancegenome.api.service.ExpressionService;
import org.alliancegenome.cache.repository.ExpressionCacheRepository;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.cache.repository.helper.PaginationResult;
import org.alliancegenome.core.ExpressionDetail;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.GOTerm;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.junit.Test;

public class ExpressionIT extends AbstractIT{

	@Inject
	private ExpressionCacheRepository repository;

	@Inject
	private ExpressionService expressionService;

	@Test
	public void checkAllExpressions() {
		Pagination pagination = new Pagination();
		pagination.setLimit(100000);
		List<String> geneIDs = new ArrayList<>();
		geneIDs.add("MGI:109583");

		PaginationResult<ExpressionDetail> response = repository.getExpressionAnnotations(geneIDs, "UBERON:0000924", pagination);
		System.out.println(response.getTotalNumber());
	}

	@Test
	// Test Pten from MGI for expression ribbon summary
	public void checkExpressionRibbonHeader() {
		RibbonSummary summary = expressionService.getExpressionRibbonSummary(List.of("MGI:109583"));
		assertNotNull(summary);
		assertEquals(summary.getDiseaseRibbonEntities().size(), 1);
		EntitySubgroupSlim slim = (EntitySubgroupSlim) summary.getDiseaseRibbonEntities().get(0)
				.getSlims().get("UBERON:0001062").get("ALL");
		assertThat(slim.getNumberOfAnnotations(), greaterThan(150));
		assertThat(slim.getNumberOfClasses(), greaterThan(90));

		// nervous system
		slim = (EntitySubgroupSlim) summary.getDiseaseRibbonEntities().get(0)
				.getSlims().get("UBERON:0001016").get("ALL");
		assertEquals(slim.getNumberOfAnnotations(), 51);
		assertEquals(slim.getNumberOfClasses(), 33);

		// post-juvenile adult stage
		slim = (EntitySubgroupSlim) summary.getDiseaseRibbonEntities().get(0)
				.getSlims().get("UBERON:0000113").get("ALL");
		assertThat(slim.getNumberOfAnnotations(), greaterThan(25));
		assertThat(slim.getNumberOfClasses(), greaterThan(0));
	}

	@Test
	// Test Pten from MGI for expression ribbon summary
	public void checkExpressionRibbonNumbers() {
		RibbonSummary summary = expressionService.getExpressionRibbonSummary(List.of("MGI:98834"));
		assertNotNull(summary);
	}

	@Test
	public void checkExpressionRibbonAvailableAttribute() {
		RibbonSummary summary = expressionService.getExpressionRibbonSummary(List.of("SGD:S000005442"));
		assertNotNull(summary);
		Map<String, Object> map = summary.getDiseaseRibbonEntities().get(0).getSlims().values().stream().skip(1).findFirst().orElse(null);
		assertEquals(((EntitySubgroupSlim) map.values().stream().findFirst().get()).getId(), "UBERON:0005409");
		assertNotNull(map.values().stream().findFirst().get());
	}

	@Test
	// Test Pten from MGI for expression ribbon summary
	public void checkExpressionRibbonNumbersManyGenes() {
		RibbonSummary summary = expressionService.getExpressionRibbonSummary(List.of("MGI:98834"));
		assertNotNull(summary);
	}

	@Test
	// Test Pten from MGI for expression ribbon summary
	public void checkExpressionRibbonNumbersWorm() {
		RibbonSummary summary = expressionService.getExpressionRibbonSummary(List.of("WB:WBGene00002881"));
		assertNotNull(summary);

		assertEquals(summary.getDiseaseRibbonEntities().size(), 1);
		EntitySubgroupSlim slim = (EntitySubgroupSlim) summary.getDiseaseRibbonEntities().get(0)
				.getSlims().get("GO:0005634").get("ALL");
		assertEquals(slim.getNumberOfAnnotations(), 4);
		assertEquals(slim.getNumberOfClasses(), 3);
	}

	@Test
	// Test Pten from MGI for expression ribbon summary
	public void checkExpressionRibbonGoTerms() {
		GeneRepository geneRepository = new GeneRepository();
		List<GOTerm> terms = geneRepository.getFullGoTermList();
		assertNotNull(terms);
		String termNames = terms.stream().map(GOTerm::getName).collect(Collectors.joining(","));
		assertTrue(termNames.contains("extracellular region"));


	}

	@Test
	// Test Pten from MGI for expression ribbon summary
	public void checkExpressionFiltering() {
		Pagination pagination = new Pagination();
		BaseFilter filter = new BaseFilter();
		filter.addFieldFilter(FieldFilter.SOURCE, "9913");
		pagination.setFieldFilterValueMap(filter);
		JsonResultResponse<ExpressionDetail> summary = expressionService.getExpressionDetails(List.of("WB:WBGene00000898"), null, pagination);
		assertNotNull(summary);


	}

	@Test
	public void checkExpressionAnatomy() {
		Pagination pagination = new Pagination();
		BaseFilter filter = new BaseFilter();
		//filter.addFieldFilter(FieldFilter.SOURCE, "9913");
		pagination.setFieldFilterValueMap(filter);
		JsonResultResponse<ExpressionDetail> summary = expressionService.getExpressionDetails(List.of("ZFIN:ZDB-GENE-030131-845"), "UBERON:0001062", pagination);
		assertNotNull(summary);
	}

	@Test
	public void checkExpressionNoResultDistinctFieldValues() {
		Pagination pagination = new Pagination();
		BaseFilter filter = new BaseFilter();
		//filter.addFieldFilter(FieldFilter.SOURCE, "9913");
		pagination.setFieldFilterValueMap(filter);
		pagination.addFieldFilter(FieldFilter.FREFERENCE, "foot");
		JsonResultResponse<ExpressionDetail> summary = expressionService.getExpressionDetails(List.of("RGD:2129"), null, pagination);
		assertNotNull(summary);
		assertEquals(summary.getTotal(), 0);
		assertNotNull(summary.getDistinctFieldValues());
		// Have at least one species value in the distinct value map.
		assertEquals(summary.getDistinctFieldValues().values().size(), 1);
	}

}
