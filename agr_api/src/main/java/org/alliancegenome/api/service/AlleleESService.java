package org.alliancegenome.api.service;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.alliancegenome.cache.repository.helper.JsonResultResponse.DISTINCT_FIELD_VALUES;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.curation_api.model.document.es.AlleleSummaryDocument;
import org.alliancegenome.curation_api.model.document.es.ESDocument;
import org.alliancegenome.curation_api.model.document.es.TransgenicAlleleDocument;
import org.alliancegenome.curation_api.model.document.es.VariantSummaryDocument;
import org.alliancegenome.es.model.query.Pagination;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class AlleleESService extends ESService {

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
		put("variants.variantType.name.sort", SortOrder.ASC);
		put("symbol.sort", SortOrder.ASC);
	}};

	LinkedHashMap<String, SortOrder> molecualarConsequenceSortMap = new LinkedHashMap<>() {{
		put("variants.curatedVariantGenomicLocations.predictedVariantConsequences.vepConsequences.name.sort", SortOrder.ASC);
		put("variants.variantType.name.sort", SortOrder.ASC);
		put("symbol.sort", SortOrder.ASC);
	}};

	LinkedHashMap<String, SortOrder> alleleSymbolSortMap = new LinkedHashMap<>() {{
		put("symbol.sort", SortOrder.ASC);
	}};

	Map<String, LinkedHashMap<String, SortOrder>> sortMap = new HashMap<>() {{
		put("default", defaultSortMap);
		put("variant", variantSortMap);
		put("variantType", variantTypeSortMap);
		put("molecularConsequence", molecualarConsequenceSortMap);
		put("alleleSymbol", alleleSymbolSortMap);
		put(null, defaultSortMap);
	}};

	public JsonResultResponse<TransgenicAlleleDocument> getTransgenicAlleles(String alleleId) {
		BoolQueryBuilder bool = boolQuery();
		BoolQueryBuilder bool2 = boolQuery();
		bool.must(bool2);
		// ToDo: Change this class such that the category is public
		// TransgenicAlleleDocument.category
		bool.filter(new TermQueryBuilder("category", "transgenic_allele_summary"));
		bool2.should(new MatchQueryBuilder("allele.primaryExternalId.keyword", alleleId));

		JsonResultResponse<TransgenicAlleleDocument> ret = new JsonResultResponse<>();

		SearchResponse searchResponse = getSearchResponse(bool, new Pagination(), null, false);
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);
		List<TransgenicAlleleDocument> list = new ArrayList<>();
		Arrays.stream(searchResponse.getHits().getHits()).forEach(searchHit -> {
			try {
				TransgenicAlleleDocument object = mapper.readValue(searchHit.getSourceAsString(), TransgenicAlleleDocument.class);
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
		SearchResponse searchResponse = getSearchResponse(bool, pagination, null, false);
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
		shouldQueryBuilder.should(QueryBuilders.termQuery("category.keyword", "variant_summary"));
		queryBuilder.must(shouldQueryBuilder);

		JsonResultResponse<ESDocument> ret = new JsonResultResponse<>();
		ret.setSupplementalData(getAlleleSupplementalData(queryBuilder));
		addTableFilter(pagination, queryBuilder);
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

	private Map<String, Object> getAlleleSupplementalData(BoolQueryBuilder unfilteredQuery) {

		Map<String, String> aggregationFields = new HashMap<>();
		aggregationFields.put("alterationType.keyword", "alleleCategory");
		aggregationFields.put("variants.variantType.name.keyword", "variantType");
		aggregationFields.put("variants.curatedVariantGenomicLocations.predictedVariantConsequences.vepConsequences.name.keyword", "molecularConsequence");

		Map<String, List<String>> distinctFieldValueMap = getAggregations(unfilteredQuery, aggregationFields, null, false, false);
		Map<String, Object> supplementalData = new LinkedHashMap<>();
		supplementalData.put(DISTINCT_FIELD_VALUES, distinctFieldValueMap);
		return supplementalData;
	}
}
