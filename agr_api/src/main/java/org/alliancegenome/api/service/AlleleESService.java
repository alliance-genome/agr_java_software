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
import org.alliancegenome.curation_api.model.document.es.TransgenicAlleleDocument;
import org.alliancegenome.curation_api.model.document.es.VariantSummaryDocument;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class AlleleESService extends ESService {

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

	public JsonResultResponse<AlleleSummaryDocument> getAllelesByGene(String geneId, Pagination pagination) {

		BoolQueryBuilder bool = boolQuery();
		bool.must(new MatchQueryBuilder("alleleOfGene.primaryExternalId", geneId));
		bool.filter(new TermQueryBuilder("category", "allele_summary"));
		JsonResultResponse<AlleleSummaryDocument> ret = new JsonResultResponse<>();
		ret.setSupplementalData(getAlleleSupplementalData(bool));
		addTableFilter(pagination, bool);
		LinkedHashMap<String, SortOrder> sortOrders = getAlleleSortOrders(pagination);
		SearchResponse searchResponse = getSearchResponse(bool, pagination, sortOrders, false);
		List<AlleleSummaryDocument> list = new ArrayList<>();
		Arrays.stream(searchResponse.getHits().getHits()).forEach(searchHit -> {
			try {
				AlleleSummaryDocument object = mapper.readValue(searchHit.getSourceAsString(), AlleleSummaryDocument.class);
				list.add(object);
			} catch (Exception e) {
				e.printStackTrace();
			}
		});
		
		ret.setResults(list);
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);
		return ret;
	}

	private LinkedHashMap<String, SortOrder> getAlleleSortOrders(Pagination pagination) {
		
		LinkedHashMap<String, SortOrder> sortingMap = new LinkedHashMap<>();
		LinkedHashMap<String, String> sortingSetMap = new LinkedHashMap<>();

		//sortingSetMap.put("default", "geneExpressionAnnotation.expressionAnnotationSubject.taxon.name.keyword");
		sortingSetMap.put("variant", "variants.curatedVariantGenomicLocations.hgvs.sort");
		sortingSetMap.put("variantType", "variants.variantType.name.sort");
		sortingSetMap.put("alleleSymbol", "allele.alleleSymbol.displayText.sort");
		sortingSetMap.put("molecularConsequence", "variants.curatedVariantGenomicLocations.predictedVariantConsequences.vepConsequences.name.sort");
		sortingSetMap.put("alterationType","alterationType.sort");
		sortingSetMap.put("hasPhenotype","hasPhenotype");
		sortingSetMap.put("hasDisease","hasDisease");

		String sortField = pagination.getSortBy() != null ? pagination.getSortBy() : "default";

		switch (sortField) {
			case "default" :
				sortingMap.put(sortingSetMap.get("hasPhenotype"), SortOrder.DESC);
				sortingMap.put(sortingSetMap.get("hasDisease"), SortOrder.DESC);
				sortingMap.put(sortingSetMap.get("alterationType"), SortOrder.ASC);
				break;
			case "variant" :
				sortingMap.put(sortingSetMap.get("variant"), SortOrder.ASC);
				sortingMap.put(sortingSetMap.get("alleleSymbol"), SortOrder.ASC);
				break;
			case "variantType" :
				sortingMap.put(sortingSetMap.get("variantType"), SortOrder.ASC);
				sortingMap.put(sortingSetMap.get("alleleSymbol"), SortOrder.ASC);
				break;
			case "molecularConsequence" :
				sortingMap.put(sortingSetMap.get("molecularConsequence"), SortOrder.ASC);
				sortingMap.put(sortingSetMap.get("variantType"), SortOrder.ASC);
				sortingMap.put(sortingSetMap.get("alleleSymbol"), SortOrder.ASC);
				break;
			case "alleleSymbol" :
				sortingMap.put(sortingSetMap.get("alleleSymbol"), SortOrder.ASC);
		}
		
		return sortingMap;
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
