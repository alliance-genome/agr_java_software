package org.alliancegenome.api.tests.integration;

import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.List;
import java.util.Map;

import org.alliancegenome.api.dto.EntitySubgroupSlim;
import org.alliancegenome.api.dto.RibbonSummary;
import org.alliancegenome.api.service.ExpressionESService;
import org.alliancegenome.api.service.ExpressionRibbonESService;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.curation_api.model.document.es.GeneExpressionDocument;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.view.BaseFilter;
import org.junit.Test;

import jakarta.inject.Inject;

public class ExpressionIT extends AbstractIT {

	@Inject private ExpressionRibbonESService expressionService;

	@Inject private ExpressionESService expressionESService;

	@Test
	// Test Pten from MGI for expression ribbon summary
	public void checkExpressionRibbonHeader() {
		RibbonSummary summary = expressionService.getExpressionRibbonSummary(List.of("MGI:109583"));
		assertNotNull(summary);
		assertEquals(summary.getDiseaseRibbonEntities().size(), 1);
		EntitySubgroupSlim slim = (EntitySubgroupSlim) summary.getDiseaseRibbonEntities().get(0).getSlims().get("UBERON:0001062").get("ALL");
		assertThat(slim.getNumberOfAnnotations(), greaterThan(150));
		assertThat(slim.getNumberOfClasses(), greaterThan(90));

		// nervous system
		slim = (EntitySubgroupSlim) summary.getDiseaseRibbonEntities().get(0).getSlims().get("UBERON:0001016").get("ALL");
		assertEquals(slim.getNumberOfAnnotations(), 51);
		assertEquals(slim.getNumberOfClasses(), 33);

		// post-juvenile adult stage
		slim = (EntitySubgroupSlim) summary.getDiseaseRibbonEntities().get(0).getSlims().get("UBERON:0000113").get("ALL");
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
		EntitySubgroupSlim slim = (EntitySubgroupSlim) summary.getDiseaseRibbonEntities().get(0).getSlims().get("GO:0005634").get("ALL");
		assertEquals(slim.getNumberOfAnnotations(), 4);
		assertEquals(slim.getNumberOfClasses(), 3);
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
