package org.alliancegenome.core.variant.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.document.es.SequenceSummaryDocument;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.es.util.EsClientFactory;
import org.apache.lucene.search.SortField;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class AlleleVariantIndexService {

	@Inject ObjectMapper mapper;
	
	/**
	 * DETAIL PAGE
	 */
	public JsonResultResponse<SequenceSummaryDocument> getAllelesNVariants(String geneId, Pagination pagination) {
		SearchResponse searchResponse = null;
		try {
			Log.debug("BEFORE QUERY:" + new Date());

			SearchSourceBuilder srb = new SearchSourceBuilder();

			BoolQueryBuilder queryBuilder = new BoolQueryBuilder();
			BoolQueryBuilder geneQuery = new BoolQueryBuilder();
			geneQuery.should(QueryBuilders.termQuery("geneIds.keyword", geneId));
			geneQuery.should(QueryBuilders.termQuery("variant.overlapGenes.curie.keyword", geneId));
			queryBuilder.must(geneQuery);
			queryBuilder.must(QueryBuilders.termQuery("category.keyword", "sequence_summary"));

			srb.query(queryBuilder);
			srb.sort(new FieldSortBuilder(getSortFields(pagination)[0].getField()).order(SortOrder.ASC));
			srb.from(pagination.getStart());
			srb.size(pagination.getLimit());
			srb.trackTotalHits(true);

			SearchRequest searchRequest = new SearchRequest(ConfigHelper.getEsIndex());

			searchRequest.source(srb);
			Log.info("Search Request: " + searchRequest);
			searchResponse = EsClientFactory.getDefaultEsClient().search(searchRequest, EsClientFactory.LARGE_RESPONSE_REQUEST_OPTIONS);

			Log.info("Len: " + searchResponse.getHits().getHits().length);
			Log.info("AFTER QUERY:" + new Date() + "\tTOOK:" + searchResponse.getTook());

		} catch (IOException e) {
			e.printStackTrace();
		}
		List<SequenceSummaryDocument> docList = new ArrayList<>();
		if (searchResponse != null) {
			for (SearchHit searchHit : searchResponse.getHits().getHits()) {
				try {
					SequenceSummaryDocument doc = mapper.readValue(searchHit.getSourceAsString(), SequenceSummaryDocument.class);
					doc.setCategory("variant"); // will need to change this once we get the other file types
					docList.add(doc);
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
			}
		}
		Log.info("TOTAL HITS:" + searchResponse.getHits().getTotalHits().value);
		Log.info("Sequence Summary Documents:" + docList.size());
		
		JsonResultResponse<SequenceSummaryDocument> response = new JsonResultResponse<>();
		response.setResults(docList);
		response.setTotal(searchResponse.getHits().getTotalHits().value);
		response.addDistinctFieldValueSupplementalData(getAggregations(geneId));
		
		return response;

	}

	public SortField[] getSortFields(Pagination pagination) {
		SortField[] sortField = new SortField[2];

		if (pagination.getSortBy() != null && !pagination.getSortBy().equalsIgnoreCase("default")) {
			if (pagination.getSortBy().equalsIgnoreCase("variantType")) {
				sortField[0] = new SortField("variantType.keyword", SortField.Type.STRING);
			}
			if (pagination.getSortBy().equalsIgnoreCase("molecularConsequence")) {
				sortField[0] = new SortField("molecularConsequence.keyword", SortField.Type.STRING);
			}
			if (pagination.getSortBy().equalsIgnoreCase("VARIANT")) {
				sortField[0] = new SortField("allele.variants.displayName.keyword", SortField.Type.STRING);
			}
			if (pagination.getSortBy().equalsIgnoreCase("transcript")) {
				sortField[0] = new SortField("allele.variants.transcriptLevelConsequence.transcript.name.keyword", SortField.Type.STRING);
			}
			if (pagination.getSortBy().equalsIgnoreCase("VariantHgvsName") || pagination.getSortBy().equalsIgnoreCase("symbol")) {
				sortField[0] = new SortField("allele.variants.hgvsG.keyword", SortField.Type.STRING);
			}
		} else {
			sortField[0] = new SortField("alterationType.keyword", SortField.Type.STRING);
		}
		return sortField;
	}

	public Map<String, List<String>> getAggregations(String geneId) {
		SearchRequest searchRequest = new SearchRequest(ConfigHelper.getEsIndex());
		SearchResponse searchResponse = null;

		SearchSourceBuilder srb = new SearchSourceBuilder();
		srb.size(0);
		srb.query(buildBoolQuery(geneId, null));

		buildAggregations(srb);

		searchRequest.source(srb);
		try {
			searchResponse = EsClientFactory.getDefaultEsClient().search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			e.printStackTrace();
		}

		Map<String, List<String>> aggregations = new HashMap<>();
		Terms typeAggs = null;
		if (searchResponse != null && searchResponse.getAggregations() != null) {
			typeAggs = searchResponse.getAggregations().get("variantType");

			aggregations.put("filter.variantType", new ArrayList<>());
			if (typeAggs != null) {
				for (Terms.Bucket b : typeAggs.getBuckets()) {
					if (!b.getKey().toString().equals("")) {
						aggregations.get("filter.variantType").add((String) b.getKey());
					}
				}
			}
			Terms hasDiseaseAggs = searchResponse.getAggregations().get("hasDisease");
			aggregations.put("filter.hasDisease", new ArrayList<>());
			for (Terms.Bucket b : hasDiseaseAggs.getBuckets()) {
				if (!b.getKey().toString().equals("")) {
					aggregations.get("filter.hasDisease").add((String) b.getKey());
				}
			}
			Terms hasPhenotype = searchResponse.getAggregations().get("hasPhenotype");
			aggregations.put("filter.hasPhenotype", new ArrayList<>());
			for (Terms.Bucket b : hasPhenotype.getBuckets()) {
				if (!b.getKey().toString().equals("")) {
					aggregations.get("filter.hasPhenotype").add((String) b.getKey());
				}
			}
			Terms molecularConsequences = searchResponse.getAggregations().get("molecularConsequences");
			aggregations.put("filter.molecularConsequence", new ArrayList<>());
			for (Terms.Bucket b : molecularConsequences.getBuckets()) {
				if (!b.getKey().toString().equals("")) {
					aggregations.get("filter.molecularConsequence").add((String) b.getKey());
				}
			}

			Terms alterationType = searchResponse.getAggregations().get("alterationType");
			aggregations.put("filter.alleleCategory", new ArrayList<>());
			for (Terms.Bucket b : alterationType.getBuckets()) {
				if (!b.getKey().toString().equals("")) {
					aggregations.get("filter.alleleCategory").add((String) b.getKey());
				}
			}
		}
		return aggregations;
	}
	
	public BoolQueryBuilder buildBoolQuery(String geneId, Pagination pagination) {
		BoolQueryBuilder queryBuilder = new BoolQueryBuilder();
		queryBuilder.filter(QueryBuilders.termQuery("geneIds.keyword", geneId));
		queryBuilder.filter(QueryBuilders.termsQuery("category", "allele"));
		if (pagination != null) {
			HashMap<FieldFilter, String> filterValueMap = pagination.getFieldFilterValueMap();

			if (filterValueMap != null) {
				for (Map.Entry e : filterValueMap.entrySet()) {
					if (e.getKey().toString().equalsIgnoreCase("symbol")) {
						queryBuilder.must(QueryBuilders.wildcardQuery("symbol", "*" + e.getValue().toString() + "*"));

					}
					if (e.getKey().toString().equalsIgnoreCase("synonyms")) {
						queryBuilder.must(QueryBuilders.wildcardQuery("allele.synonyms", "*" + e.getValue().toString() + "*"));
					}
					if (e.getKey().toString().equalsIgnoreCase("allele_category")) {
						queryBuilder.filter(QueryBuilders.termsQuery("alterationType.keyword", e.getValue().toString().split("\\|")));
					}
					if (e.getKey().toString().equalsIgnoreCase("variant_type")) {
						queryBuilder.must(QueryBuilders.termsQuery("variantType.keyword", e.getValue().toString().split("\\|")));
					}
					if (e.getKey().toString().equalsIgnoreCase("has_disease")) {
						queryBuilder.filter(QueryBuilders.termsQuery("allele.hasDisease", e.getValue().toString().split("\\|")));
					}
					if (e.getKey().toString().equalsIgnoreCase("molecular_consequence")) {
						queryBuilder.filter(QueryBuilders.termsQuery("allele.variants.transcriptLevelConsequence.molecularConsequences.keyword", e.getValue().toString().split("\\|")));
					}
					if (e.getKey().toString().equalsIgnoreCase("HAS_PHENOTYPE")) {
						queryBuilder.filter(QueryBuilders.termsQuery("allele.hasPhenotype", e.getValue().toString().split("\\|")));
					}
				}
			}
		}
		return queryBuilder;
	}
	
	
	public void buildAggregations(SearchSourceBuilder srb) {
		srb.aggregation(AggregationBuilders.terms("variantType").field("variantType.keyword"));
		srb.aggregation(AggregationBuilders.terms("hasDisease").field("hasDisease.keyword"));
		srb.aggregation(AggregationBuilders.terms("hasPhenotype").field("allele.hasPhenotype.keyword"));
		srb.aggregation(AggregationBuilders.terms("impact").field("allele.variants.transcriptLevelConsequence.impact.keyword"));
		srb.aggregation(AggregationBuilders.terms("polyphenPrediction").field("allele.variants.transcriptLevelConsequence.polyphenPrediction.keyword"));
		srb.aggregation(AggregationBuilders.terms("siftPrediction").field("allele.variants.transcriptLevelConsequence.siftPrediction.keyword"));
		srb.aggregation(AggregationBuilders.terms("sequenceFeatureType").field("allele.variants.transcriptLevelConsequence.sequenceFeatureType.keyword"));
		srb.aggregation(AggregationBuilders.terms("transcriptName").field("allele.variants.transcriptLevelConsequence.transcript.name.keyword"));
		srb.aggregation(AggregationBuilders.terms("allele.gene.symbol").field("allele.gene.symbol"));
		srb.aggregation(AggregationBuilders.terms("location").field("allele.variants.transcriptLevelConsequence.location.keyword"));
		srb.aggregation(AggregationBuilders.terms("molecularConsequences").field("allele.variants.transcriptLevelConsequence.molecularConsequences.keyword"));
		srb.aggregation(AggregationBuilders.terms("alterationType").field("alterationType.keyword"));
	}
	
}