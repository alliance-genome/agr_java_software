package org.alliancegenome.api.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.alliancegenome.api.es.query.Pagination;
import org.alliancegenome.api.response.JsonResultResponse;
import org.alliancegenome.api.translators.tdf.AlleleToTdfTranslator;
import org.alliancegenome.curation_api.model.document.es.AlleleSummaryDocument;
import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.curation_api.model.document.es.VariantSummaryDocument;
import org.apache.lucene.search.TotalHits;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchResponse.Clusters;
import org.elasticsearch.action.search.SearchResponseSections;
import org.elasticsearch.action.search.ShardSearchFailure;
import org.elasticsearch.common.bytes.BytesArray;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.sort.SortOrder;
import org.junit.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AlleleESServiceTest {

	@Test
	public void resolvesIdentifiersInTheDocumentedFallbackOrder() {
		assertEquals("CURIE:1", AlleleESService.resolveAlleleIdentifier(source(
			Map.of("curie", "CURIE:1", "primaryExternalId", "PRIMARY:1", "modInternalId", "MOD:1"))));
		assertEquals("PRIMARY:1", AlleleESService.resolveAlleleIdentifier(source(
			Map.of("primaryExternalId", "PRIMARY:1", "modInternalId", "MOD:1"))));
		assertEquals("MOD:1", AlleleESService.resolveAlleleIdentifier(source(
			Map.of("modInternalId", "MOD:1"))));
		assertEquals(null, AlleleESService.resolveAlleleIdentifier(Map.of()));
	}

	@Test
	public void returnsOnlyProjectedIdentifiersAndPreservesCompleteTotal() {
		CapturingAlleleESService service = new CapturingAlleleESService(
			response(5,
				hit(1, "{\"allele\":{\"primaryExternalId\":\"MGI:1\"}}"),
				hit(2, "{\"allele\":{\"curie\":\"FB:2\"}}")),
			response(3));
		Pagination pagination = new Pagination(2, 2, null, null);

		JsonResultResponse<String> result = service.getVisibleAlleleIdsByGene("MGI:gene", pagination);

		assertEquals(List.of("MGI:1", "FB:2"), result.getResults());
		assertEquals(2, result.getReturnedRecords());
		assertEquals(5, result.getTotal());
		Pagination identifierPagination = service.capturedPaginations.get(0);
		assertEquals(AlleleESService.VIEWER_SOURCE_INCLUDES, identifierPagination.getSourceIncludes());
		assertEquals(2, identifierPagination.getLimit().intValue());
		assertEquals(2, identifierPagination.getPage().intValue());

		Pagination standaloneProbePagination = service.capturedPaginations.get(1);
		assertEquals(List.of("category"), standaloneProbePagination.getSourceIncludes());
		assertEquals(1, standaloneProbePagination.getLimit().intValue());
		assertEquals(1, standaloneProbePagination.getPage().intValue());
		assertEquals(true, result.getSupplementalData().get("hasStandaloneVariants"));
	}

	@Test
	public void appliesVisibleCategoriesAndSupportedTableFiltersToBothQueries() {
		CapturingAlleleESService service = new CapturingAlleleESService(response(0), response(0));
		Pagination pagination = new Pagination(1, 1000, null, null);
		pagination.addFilterOption("symbol", "abc");
		pagination.addFilterOption("hasDisease", "true");
		pagination.addFilterOption("alterationType.keyword", "allele with one variant|allele");

		JsonResultResponse<String> result = service.getVisibleAlleleIdsByGene("ZFIN:gene", pagination);

		String identifierQuery = service.capturedQueries.get(0).toString();
		assertTrue(identifierQuery.contains("allele_summary"));
		assertTrue(identifierQuery.contains("allele with one variant"));
		assertTrue(identifierQuery.contains("allele with multiple variants"));
		assertTrue(identifierQuery.contains("*abc*"));
		assertTrue(identifierQuery.contains("hasDisease"));
		assertTrue(identifierQuery.contains("true"));
		assertTrue(identifierQuery.contains("allele"));
		assertFalse(identifierQuery.contains("variant_summary"));

		String standaloneVariantQuery = service.capturedQueries.get(1).toString();
		assertTrue(standaloneVariantQuery.contains("variant_summary"));
		assertTrue(standaloneVariantQuery.contains("variantList.curatedVariantGenomicLocations.hgvs"));
		assertTrue(standaloneVariantQuery.contains("*abc*"));
		assertTrue(standaloneVariantQuery.contains("hasDisease"));
		assertEquals(false, result.getSupplementalData().get("hasStandaloneVariants"));
	}

	@Test
	public void rejectsProjectedRecordsWithoutAnIdentifier() {
		CapturingAlleleESService service = new CapturingAlleleESService(response(1,
			hit(1, "{\"allele\":{\"primaryExternalId\":\"\"}}")));

		assertThrows(IllegalStateException.class, () ->
			service.getVisibleAlleleIdsByGene("MGI:gene", new Pagination(1, 1000, null, null)));
	}

	@Test
	public void tableProjectionContainsEveryRenderedAndDownloadedLeaf() {
		assertEquals(20, AlleleESService.TABLE_SOURCE_INCLUDES.size());
		assertTrue(AlleleESService.TABLE_SOURCE_INCLUDES.contains("allele.type"));
		assertTrue(AlleleESService.TABLE_SOURCE_INCLUDES.contains("allele.alleleSymbol.type"));
		assertTrue(AlleleESService.TABLE_SOURCE_INCLUDES.contains("allele.alleleSynonyms.type"));
		assertTrue(AlleleESService.TABLE_SOURCE_INCLUDES.contains("variantList.type"));
		assertTrue(AlleleESService.TABLE_SOURCE_INCLUDES.contains(
			"variantList.curatedVariantGenomicLocations.variantGenomicLocationAssociationObject.type"));
		assertTrue(AlleleESService.TABLE_SOURCE_INCLUDES.contains("category"));
		assertTrue(AlleleESService.TABLE_SOURCE_INCLUDES.contains("alterationType"));
		assertTrue(AlleleESService.TABLE_SOURCE_INCLUDES.contains("allele.alleleSymbol.displayText"));
		assertTrue(AlleleESService.TABLE_SOURCE_INCLUDES.contains("allele.alleleSynonyms.displayText"));
		assertTrue(AlleleESService.TABLE_SOURCE_INCLUDES.contains("variantList.variantType.name"));
		assertTrue(AlleleESService.TABLE_SOURCE_INCLUDES.contains("variantList.curatedVariantGenomicLocations.hgvs"));
		assertTrue(AlleleESService.TABLE_SOURCE_INCLUDES.contains("variantList.curatedVariantGenomicLocations.start"));
		assertTrue(AlleleESService.TABLE_SOURCE_INCLUDES.contains("variantList.curatedVariantGenomicLocations.end"));
		assertTrue(AlleleESService.TABLE_SOURCE_INCLUDES.contains(
			"variantList.curatedVariantGenomicLocations.variantGenomicLocationAssociationObject.name"));
		assertTrue(AlleleESService.TABLE_SOURCE_INCLUDES.contains(
			"variantList.curatedVariantGenomicLocations.predictedVariantConsequences.vepConsequences.name"));
		assertTrue(AlleleESService.TABLE_SOURCE_INCLUDES.contains("hasDisease"));
		assertTrue(AlleleESService.TABLE_SOURCE_INCLUDES.contains("hasPhenotype"));
	}

	@Test
	public void projectedVariantStillDeserializesAndExportsMolecularConsequence() throws Exception {
		String projectedSource = "{"
			+ "\"category\":\"variant_summary\","
			+ "\"alterationType\":\"variant\","
			+ "\"hasDisease\":false,\"hasPhenotype\":true,"
			+ "\"allele\":{\"type\":\"Allele\",\"curie\":\"rs250716330\"},"
			+ "\"variantList\":[{\"type\":\"Variant\",\"variantType\":{\"name\":\"SNP\"},"
			+ "\"curatedVariantGenomicLocations\":[{\"hgvs\":\"NC_000077.7:g.69478052C>T\","
			+ "\"start\":69478052,\"end\":69478052,"
			+ "\"variantGenomicLocationAssociationObject\":{\"type\":\"AssemblyComponent\",\"name\":\"11\"},"
			+ "\"predictedVariantConsequences\":[{\"vepConsequences\":[{\"name\":\"intron_variant\"}]}]}]}]}";

		VariantSummaryDocument document = new ObjectMapper().readValue(projectedSource, VariantSummaryDocument.class);
		String download = new AlleleToTdfTranslator().getAllRows(List.<ESDocument>of(document));

		assertTrue(download.contains("NC_000077.7:g.69478052C>T"));
		assertTrue(download.contains("SNP"));
		assertTrue(download.contains("intron_variant"));
		assertTrue(download.contains("false"));
		assertTrue(download.contains("true"));
	}

	@Test
	public void projectedAlleleStillDeserializesAndExportsSymbolAndIdentifier() throws Exception {
		String projectedSource = "{"
			+ "\"category\":\"allele_summary\",\"alterationType\":\"allele\","
			+ "\"hasDisease\":false,\"hasPhenotype\":false,"
			+ "\"allele\":{\"type\":\"Allele\",\"primaryExternalId\":\"MGI:5246506\","
			+ "\"alleleSymbol\":{\"displayText\":\"Trp53<sup>Gt(IST14609B5)Tigm</sup>\"},"
			+ "\"alleleSynonyms\":[{\"displayText\":\"Trp53 test synonym\"}]}}";

		AlleleSummaryDocument document = new ObjectMapper().readValue(projectedSource, AlleleSummaryDocument.class);
		String download = new AlleleToTdfTranslator().getAllRows(List.<ESDocument>of(document));

		assertTrue(download.contains("MGI:5246506"));
		assertTrue(download.contains("Trp53<sup>Gt(IST14609B5)Tigm</sup>"));
		assertTrue(download.contains("Trp53 test synonym"));
	}

	private static Map<String, Object> source(Map<String, String> allele) {
		return Map.of("allele", allele);
	}

	private static SearchHit hit(int docId, String source) {
		SearchHit hit = new SearchHit(docId, "hit-" + docId, null, Map.of(), Map.of());
		hit.sourceRef(new BytesArray(source));
		return hit;
	}

	private static SearchResponse response(long total, SearchHit... hits) {
		SearchHits searchHits = new SearchHits(hits, new TotalHits(total, TotalHits.Relation.EQUAL_TO), 1.0f);
		SearchResponseSections sections = new SearchResponseSections(searchHits, null, null, false, null, null, 1);
		return new SearchResponse(sections, null, 1, 1, 0, 1L, ShardSearchFailure.EMPTY_ARRAY, Clusters.EMPTY);
	}

	private static class CapturingAlleleESService extends AlleleESService {
		private final SearchResponse[] responses;
		private int responseIndex;
		private final List<Pagination> capturedPaginations = new ArrayList<>();
		private final List<BoolQueryBuilder> capturedQueries = new ArrayList<>();

		private CapturingAlleleESService(SearchResponse... responses) {
			this.responses = responses;
		}

		@Override
		protected SearchResponse getSearchResponse(BoolQueryBuilder query, Pagination pagination,
			LinkedHashMap<String, SortOrder> sorts, boolean debug) {
			capturedQueries.add(query);
			capturedPaginations.add(pagination);
			return responses[responseIndex++];
		}
	}
}
