package org.alliancegenome.es.index.site.dao;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.index.ESDAO;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.es.util.EsClientFactory;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.CrossReference;
import org.alliancegenome.neo4j.entity.node.GeneticEntity;
import org.alliancegenome.neo4j.entity.node.Variant;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.core.CountRequest;
import org.elasticsearch.client.core.CountResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class VariantESDAO extends ESDAO {

	public static final String SITE_INDEX = ConfigHelper.getEsIndex();

	public static ObjectMapper mapper = new ObjectMapper();


	public Integer performQueryCount(QueryBuilder query, Pagination pagination) {

		// index name needs to come from configuration
		CountRequest countRequest = new CountRequest(SITE_INDEX);
		countRequest.query(query);

		CountResponse response = null;
		try {
			response = EsClientFactory.getDefaultEsClient().count(countRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return response == null ? 0 : (int) response.getCount();
	}

	private static Map<String, List<String>> sortAlleles = new HashMap<>();

	static {
		sortAlleles.put("default", List.of("primaryKey.keyword"));
		sortAlleles.put("molecularConsequence", List.of("transcriptLevelConsequences.molecularConsequence.keyword", "primaryKey.keyword"));
		sortAlleles.put("variant", List.of("transcriptLevelConsequences.molecularConsequence.keyword", "primaryKey.keyword"));
	}

	public JsonResultResponse<Allele> performQuery(SearchSourceBuilder searchSourceBuilder, Pagination pagination) {

		SearchRequest searchRequest = new SearchRequest(SITE_INDEX);
		searchRequest.source(searchSourceBuilder);
		searchSourceBuilder.size(pagination.getLimit());
		// sorting
		if (sortAlleles.get(pagination.getSortBy()) != null) {
			sortAlleles.get(pagination.getSortBy()).forEach(searchSourceBuilder::sort);
		}
		SearchResponse response = null;

		try {
			response = EsClientFactory.getDefaultEsClient().search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (response == null || response.getHits() == null)
			return null;

		SearchHit[] searchHits = response.getHits().getHits();
		List<AlleleVariantSequence> results =
				Arrays.stream(searchHits)
						.map(hit -> {
							try {
								return mapper.readValue(hit.getSourceAsString(), AlleleVariantSequence.class);
							} catch (IOException e) {
								log.error("Error during deserialization ", e);
								throw new RuntimeException(e);
							}
						})
						.collect(toList());
		List<Allele> alleles = results.stream()
				.map(alleleVariantSequence -> {
					Allele allele;
					if (alleleVariantSequence.getAllele() == null) {
						allele = new Allele(alleleVariantSequence.getPrimaryKey(), GeneticEntity.CrossReferenceType.VARIANT);
						Variant variant = alleleVariantSequence.getVariant();

						allele.setSymbol(alleleVariantSequence.getPrimaryKey());
						Map<String, CrossReference> crossRefs = new HashMap<>();
						CrossReference ref = new CrossReference();
						ref.setName("");
						crossRefs.put("primary", ref);
						allele.setCrossReferenceMap(Map.copyOf(crossRefs));
					} else {
						allele = alleleVariantSequence.getAllele();
					}
					return allele;
				})
				.collect(toList());
		JsonResultResponse<Allele> resultResponse = new JsonResultResponse<>();
		resultResponse.setResults(alleles);
		resultResponse.setTotal(performQueryCount(searchSourceBuilder.query(), pagination));


		return resultResponse;
	}

	public Map<String, List<String>> getDistinctValues(SearchSourceBuilder searchSourceBuilder) {

		SearchRequest searchRequest = new SearchRequest(SITE_INDEX);
		searchRequest.source(searchSourceBuilder);
		SearchResponse response = null;

		Map<FieldFilter, String> distinctFields = new HashMap<>();
		distinctFields.put(FieldFilter.VARIANT_TYPE, "variant.variantType.name.keyword");
		distinctFields.put(FieldFilter.ALLELE_CATEGORY, "alterationType.keyword");
		distinctFields.put(FieldFilter.VARIANT_IMPACT, "transcriptLevelConsequences.impact.keyword");
		distinctFields.put(FieldFilter.MOLECULAR_CONSEQUENCE, "transcriptLevelConsequences.molecularConsequences.keyword");
		distinctFields.put(FieldFilter.VARIANT_SIFT, "transcriptLevelConsequences.siftPrediction.keyword");
		distinctFields.put(FieldFilter.VARIANT_POLYPHEN, "transcriptLevelConsequences.polyphenPrediction.keyword");
		distinctFields.forEach((fieldFilter, esFieldName) -> searchSourceBuilder.aggregation(AggregationBuilders.terms(fieldFilter.getName()).field(esFieldName)));

		try {
			response = EsClientFactory.getDefaultEsClient().search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// get Distinct values
		// for now on the filtered result set. This needs to be done on the full, unfiltered result set.

		Map<String, List<String>> distinctValueMap = new HashMap<>();
		for (FieldFilter filter : distinctFields.keySet()) {
			List<String> list = ((ParsedStringTerms) response.getAggregations().get(filter.getName())).getBuckets()
					.stream()
					.map(bucket -> (String) bucket.getKey())
					.collect(toList());
			distinctValueMap.put(filter.getName(), list);
		}
		return distinctValueMap;
	}

	public Map<String, Map<String, Integer>> getHistogram(SearchSourceBuilder searchSourceBuilder) {

		SearchRequest searchRequest = new SearchRequest(SITE_INDEX);
		searchRequest.source(searchSourceBuilder);
		SearchResponse response = null;

		Map<FieldFilter, String> distinctFields = new HashMap<>();
		distinctFields.put(FieldFilter.MODEL_NAME, "transcriptLevelConsequences.geneLevelConsequence.keyword");
		distinctFields.put(FieldFilter.DETECTION_METHOD, "transcriptLevelConsequences.molecularConsequence.keyword");
		distinctFields.put(FieldFilter.VARIANT_TYPE, "variant.variantType.name.keyword");
		distinctFields.put(FieldFilter.ALLELE_CATEGORY, "alterationType.keyword");
		distinctFields.put(FieldFilter.MOLECULAR_CONSEQUENCE, "transcriptLevelConsequences.impact.keyword");
		distinctFields.put(FieldFilter.ASSAY, "transcriptLevelConsequences.siftPrediction.keyword");
		distinctFields.put(FieldFilter.VARIANT_POLYPHEN, "transcriptLevelConsequences.polyphenPrediction.keyword");
		distinctFields.forEach((fieldFilter, esFieldName) -> searchSourceBuilder.aggregation(AggregationBuilders.terms(fieldFilter.getName()).field(esFieldName)));

		try {
			response = EsClientFactory.getDefaultEsClient().search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// get Distinct values
		// for now on the filtered result set. This needs to be done on the full, unfiltered result set.

		Map<String, Map<String, Integer>> distinctValueMap = new HashMap<>();
		for (FieldFilter filter : distinctFields.keySet()) {
			Map<String, Integer> map = ((ParsedStringTerms) response.getAggregations().get(filter.getName())).getBuckets()
					.stream()
/*
					.map(bucket -> {
						Map<String, Integer> map = new HashMap<>();
						map.put(bucket.getKey(), (Integer) bucket.getDocCount());
						return map;
					}
*/
					.collect(toMap(t -> (String) t.getKey(), bucket -> (int) bucket.getDocCount()));
			distinctValueMap.put(filter.getName(), map);
		}
		return distinctValueMap;
	}

	public Variant getVariant(String id) {


		BoolQueryBuilder bool = boolQuery();
		bool.filter(new TermQueryBuilder("category", "allele"));
		bool.must(new TermQueryBuilder("allele.variants.id.keyword", id));

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(bool);

		SearchRequest searchRequest = new SearchRequest(SITE_INDEX);
		searchRequest.source(searchSourceBuilder);
		SearchResponse response = null;

		try {
			response = EsClientFactory.getDefaultEsClient().search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (response == null || response.getHits() == null)
			return null;

		SearchHit[] searchHits = response.getHits().getHits();
		List<AlleleVariantSequence> results =
				Arrays.stream(searchHits)
						.map(hit -> {
							try {
								return mapper.readValue(hit.getSourceAsString(), AlleleVariantSequence.class);
							} catch (IOException e) {
								log.error("Error during deserialization ", e);
								throw new RuntimeException(e);
							}
						})
						.collect(toList());
		List<Allele> alleles = results.stream()
				.map(alleleVariantSequence -> {
					Allele allele;
					if (alleleVariantSequence.getAllele() == null) {
						allele = new Allele(alleleVariantSequence.getPrimaryKey(), GeneticEntity.CrossReferenceType.VARIANT);
						Variant variant = alleleVariantSequence.getVariant();
						allele.setVariants(List.of(variant));
						allele.setSymbol(alleleVariantSequence.getPrimaryKey());
						Map<String, CrossReference> crossRefs = new HashMap<>();
						CrossReference ref = new CrossReference();
						ref.setName("");
						crossRefs.put("primary", ref);
						allele.setCrossReferenceMap(Map.copyOf(crossRefs));
					} else {
						allele = alleleVariantSequence.getAllele();
					}
					return allele;
				})
				.collect(toList());

		return alleles.get(0).getVariants().get(0);
	}
}
