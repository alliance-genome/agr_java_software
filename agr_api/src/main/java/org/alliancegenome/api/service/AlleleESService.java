package org.alliancegenome.api.service;

import static org.alliancegenome.api.response.JsonResultResponse.DISTINCT_FIELD_VALUES;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.alliancegenome.core.document.TransgenicAlleleSummaryDocument;
import org.alliancegenome.api.response.JsonResultResponse;
import org.alliancegenome.curation_api.model.document.es.AlleleSummaryDocument;
import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.curation_api.model.document.es.VariantSummaryDocument;
import org.alliancegenome.api.es.query.Pagination;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;

import jakarta.enterprise.context.RequestScoped;
import lombok.extern.slf4j.Slf4j;

@RequestScoped
@Slf4j
public class AlleleESService extends ESService {

	static final List<String> VIEWER_SOURCE_INCLUDES = List.of(
		"allele.curie",
		"allele.primaryExternalId",
		"allele.modInternalId"
	);

	static final List<String> TABLE_SOURCE_INCLUDES = List.of(
		"category",
		"alterationType",
		"allele.type",
		"allele.curie",
		"allele.primaryExternalId",
		"allele.modInternalId",
		"allele.alleleSymbol.type",
		"allele.alleleSymbol.displayText",
		"allele.alleleSynonyms.type",
		"allele.alleleSynonyms.displayText",
		"variantList.type",
		"variantList.variantType.name",
		"variantList.curatedVariantGenomicLocations.hgvs",
		"variantList.curatedVariantGenomicLocations.start",
		"variantList.curatedVariantGenomicLocations.end",
		"variantList.curatedVariantGenomicLocations.variantGenomicLocationAssociationObject.type",
		"variantList.curatedVariantGenomicLocations.variantGenomicLocationAssociationObject.name",
		"variantList.curatedVariantGenomicLocations.predictedVariantConsequences.vepConsequences.name",
		"hasDisease",
		"hasPhenotype"
	);

	static final List<String> VISIBLE_ALTERATION_TYPES = List.of(
		"allele with one variant",
		"allele with multiple variants"
	);

	LinkedHashMap<String, SortOrder> defaultSortMap = new LinkedHashMap<>() {{
		put("hasPhenotype", SortOrder.DESC);
		put("hasDisease", SortOrder.DESC);
		put("alterationTypeSortOrder.sort", SortOrder.ASC);
		put("symbol.sort", SortOrder.ASC);
	}};

	LinkedHashMap<String, SortOrder> variantSortMap = new LinkedHashMap<>() {{
		put("symbol.sort", SortOrder.ASC);
	}};

	LinkedHashMap<String, SortOrder> variantTypeSortMap = new LinkedHashMap<>() {{
		put("variantList.variantType.name.sort", SortOrder.ASC);
		put("symbol.sort", SortOrder.ASC);
	}};

	LinkedHashMap<String, SortOrder> molecualarConsequenceSortMap = new LinkedHashMap<>() {{
		put("variantList.curatedVariantGenomicLocations.predictedVariantConsequences.vepConsequences.name.sort", SortOrder.ASC);
		put("variantList.variantType.name.sort", SortOrder.ASC);
		put("symbol.sort", SortOrder.ASC);
	}};

	LinkedHashMap<String, SortOrder> alleleSymbolSortMap = new LinkedHashMap<>() {{
		put("symbol.sort", SortOrder.ASC);
	}};

	LinkedHashMap<String, SortOrder> variantSummaryDefaultSort = new LinkedHashMap<>() {{
		put("variantList.curatedVariantGenomicLocations.variantGenomicLocationAssociationObject.name.sort", SortOrder.ASC);
		put("variantList.curatedVariantGenomicLocations.start", SortOrder.ASC);
	}};

	LinkedHashMap<String, SortOrder> viewerSortMap = new LinkedHashMap<>() {{
		put("symbol.sort", SortOrder.ASC);
		put("allele.primaryExternalId.keyword", SortOrder.ASC);
		put("_doc", SortOrder.ASC);
	}};

	Map<String, LinkedHashMap<String, SortOrder>> sortMap = new HashMap<>() {{
		put("default", defaultSortMap);
		put("variant", variantSortMap);
		put("variantType", variantTypeSortMap);
		put("molecularConsequence", molecualarConsequenceSortMap);
		put("alleleSymbol", alleleSymbolSortMap);
		put(null, defaultSortMap);
	}};

	public JsonResultResponse<TransgenicAlleleSummaryDocument> getTransgenicAlleles(String alleleId) {
		BoolQueryBuilder bool = boolQuery();
		BoolQueryBuilder bool2 = boolQuery();
		bool.must(bool2);
		// ToDo: Change this class such that the category is public
		// TransgenicAlleleSummaryDocument.category
		bool.filter(new TermQueryBuilder("category", "transgenic_allele_summary"));
		bool2.should(new MatchQueryBuilder("allele.primaryExternalId.keyword", alleleId));

		JsonResultResponse<TransgenicAlleleSummaryDocument> ret = new JsonResultResponse<>();

		SearchResponse searchResponse = getSearchResponse(bool, new Pagination(), null, false);
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);
		List<TransgenicAlleleSummaryDocument> list = new ArrayList<>();
		Arrays.stream(searchResponse.getHits().getHits()).forEach(searchHit -> {
			try {
				TransgenicAlleleSummaryDocument object = mapper.readValue(searchHit.getSourceAsString(), TransgenicAlleleSummaryDocument.class);
				list.add(object);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		ret.setResults(list);
		return ret;
	}

	public AlleleSummaryDocument getById(String alleleId) {

		BoolQueryBuilder bool = boolQuery();
		bool.must(new MatchQueryBuilder("allele.primaryExternalId", alleleId));
		bool.filter(new TermQueryBuilder("category", "allele_summary"));
		Pagination pagination = new Pagination();
		SearchResponse searchResponse = getSearchResponse(bool, pagination, null, false);
		try {
			if (searchResponse.getHits().getTotalHits().value >= 1) {
				return mapper.readValue(searchResponse.getHits().getHits()[0].getSourceAsString(), AlleleSummaryDocument.class);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;

	}

	public JsonResultResponse<VariantSummaryDocument> getVariantSummary(String alleleId, Pagination pagination) {

		BoolQueryBuilder bool = boolQuery();
		bool.must(new MatchQueryBuilder("allele.primaryExternalId", alleleId));
		bool.filter(new TermQueryBuilder("category", "variant_summary"));
		SearchResponse searchResponse = getSearchResponse(bool, pagination, variantSummaryDefaultSort, false);
		List<VariantSummaryDocument> list = new ArrayList<>();
		for (SearchHit hit : searchResponse.getHits().getHits()) {
			try {
				VariantSummaryDocument object = mapper.readValue(hit.getSourceAsString(), VariantSummaryDocument.class);
				list.add(object);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		JsonResultResponse<VariantSummaryDocument> ret = new JsonResultResponse<>();
		ret.setResults(list);
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);
		return ret;
	}

	public JsonResultResponse<ESDocument> getAllelesByGene(String geneId, Pagination pagination) {

		BoolQueryBuilder queryBuilder = new BoolQueryBuilder();
		BoolQueryBuilder shouldQueryBuilder = new BoolQueryBuilder();
		queryBuilder.must(QueryBuilders.termQuery("geneIds", geneId));
		shouldQueryBuilder.should(QueryBuilders.termQuery("category.keyword", "allele_summary"));
		BoolQueryBuilder variantWithNullAllele = new BoolQueryBuilder();
		variantWithNullAllele.must(QueryBuilders.termQuery("category.keyword", "variant_summary"));
		variantWithNullAllele.mustNot(QueryBuilders.existsQuery("allele"));
		shouldQueryBuilder.should(variantWithNullAllele);
		queryBuilder.must(shouldQueryBuilder);

		JsonResultResponse<ESDocument> ret = new JsonResultResponse<>();
		ret.setSupplementalData(getAlleleSupplementalData(queryBuilder));
		addTableFilter(pagination, queryBuilder);
		pagination.setSourceIncludes(TABLE_SOURCE_INCLUDES);
		SearchResponse searchResponse = getSearchResponse(queryBuilder, pagination, sortMap.get(pagination.getSortBy()), false);
		List<ESDocument> list = new ArrayList<>();
		Arrays.stream(searchResponse.getHits().getHits()).forEach(searchHit -> {
			try {
				String category = (String) searchHit.getSourceAsMap().get("category");
				if (category.equals("allele_summary")) {
					AlleleSummaryDocument asd = mapper.readValue(searchHit.getSourceAsString(), AlleleSummaryDocument.class);
					list.add(asd);
				} else if (category.equals("variant_summary")) {
					VariantSummaryDocument vsd = mapper.readValue(searchHit.getSourceAsString(), VariantSummaryDocument.class);
					list.add(vsd);
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		
		ret.setResults(list);
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);
		return ret;
	}

	public JsonResultResponse<String> getVisibleAlleleIdsByGene(String geneId, Pagination pagination) {
		BoolQueryBuilder queryBuilder = new BoolQueryBuilder();
		queryBuilder.must(QueryBuilders.termQuery("geneIds", geneId));
		queryBuilder.filter(QueryBuilders.termQuery("category.keyword", "allele_summary"));
		queryBuilder.filter(QueryBuilders.termsQuery("alterationType.keyword", VISIBLE_ALTERATION_TYPES));
		addTableFilter(pagination, queryBuilder);
		pagination.setSourceIncludes(VIEWER_SOURCE_INCLUDES);

		SearchResponse searchResponse = getSearchResponse(queryBuilder, pagination, viewerSortMap, false);
		List<String> identifiers = new ArrayList<>();
		for (SearchHit searchHit : searchResponse.getHits().getHits()) {
			String identifier = resolveAlleleIdentifier(searchHit.getSourceAsMap());
			if (identifier == null) {
				log.error("Projected viewer record has no supported allele identifier: searchHitId={}", searchHit.getId());
				throw new IllegalStateException("Projected viewer record has no supported allele identifier");
			}
			identifiers.add(identifier);
		}

		JsonResultResponse<String> response = new JsonResultResponse<>();
		response.setResults(identifiers);
		response.setTotal(searchResponse.getHits().getTotalHits().value);
		return response;
	}

	static String resolveAlleleIdentifier(Map<String, Object> source) {
		Object alleleValue = source.get("allele");
		if (!(alleleValue instanceof Map<?, ?> allele)) {
			return null;
		}
		for (String field : List.of("curie", "primaryExternalId", "modInternalId")) {
			Object value = allele.get(field);
			if (value instanceof String identifier && !identifier.isBlank()) {
				return identifier;
			}
		}
		return null;
	}

	private Map<String, Object> getAlleleSupplementalData(BoolQueryBuilder unfilteredQuery) {

		Map<String, String> aggregationFields = new HashMap<>();
		aggregationFields.put("alterationType.keyword", "alleleCategory");
		aggregationFields.put("variantList.variantType.name.keyword", "variantType");
		aggregationFields.put("variantList.curatedVariantGenomicLocations.predictedVariantConsequences.vepConsequences.name.keyword", "molecularConsequence");

		Map<String, List<String>> distinctFieldValueMap = getAggregations(unfilteredQuery, aggregationFields, null, false, false);
		distinctFieldValueMap.values().forEach(v -> v.sort(String.CASE_INSENSITIVE_ORDER));
		Map<String, Object> supplementalData = new LinkedHashMap<>();
		supplementalData.put(DISTINCT_FIELD_VALUES, distinctFieldValueMap);
		return supplementalData;
	}
}
