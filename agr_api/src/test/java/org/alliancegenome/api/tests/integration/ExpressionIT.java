package org.alliancegenome.api.tests.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.stream.Collectors;

import org.alliancegenome.api.service.ExpressionESService;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.curation_api.model.document.es.GeneExpressionDocument;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.GOTerm;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.junit.Test;

import jakarta.inject.Inject;

public class ExpressionIT extends AbstractIT {

	@Inject private ExpressionESService expressionESService;

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
		JsonResultResponse<GeneExpressionDocument> summary = expressionESService.getExpressionAnnotations(List.of("WB:WBGene00000898"), null, "NCBITaxon:6239", pagination);
		assertNotNull(summary);

	}

	@Test
	public void checkExpressionAnatomy() {
		Pagination pagination = new Pagination();
		BaseFilter filter = new BaseFilter();
		// filter.addFieldFilter(FieldFilter.SOURCE, "9913");
		pagination.setFieldFilterValueMap(filter);
		JsonResultResponse<GeneExpressionDocument> summary = expressionESService.getExpressionAnnotations(List.of("ZFIN:ZDB-GENE-030131-845"), "UBERON:0001062", "NCBITaxon:7955", pagination);
		assertNotNull(summary);
	}

	@Test
	public void checkExpressionNoResultDistinctFieldValues() {
		Pagination pagination = new Pagination();
		BaseFilter filter = new BaseFilter();
		// filter.addFieldFilter(FieldFilter.SOURCE, "9913");
		pagination.setFieldFilterValueMap(filter);
		pagination.addFieldFilter(FieldFilter.FREFERENCE, "foot");
		JsonResultResponse<GeneExpressionDocument> summary = expressionESService.getExpressionAnnotations(List.of("RGD:2129"), null, "NCBITaxon:10116", pagination);
		assertNotNull(summary);
		assertEquals(summary.getTotal(), 0);
		assertNotNull(summary.retrieveDistinctFieldValues());
		// Have at least one species value in the distinct value map.
		assertEquals(summary.retrieveDistinctFieldValues().values().size(), 1);
	}

}
