package org.alliancegenome.core.variant.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
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

	public JsonResultResponse<SequenceSummaryDocument> getAllelesNVariants(String geneId, Pagination pagination) {
		SearchResponse searchResponse = null;
		try {
			Log.debug("BEFORE QUERY:" + new Date());

			SearchSourceBuilder srb = new SearchSourceBuilder();

			srb.query(buildBoolQuery(geneId, pagination));
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
				sortField[0] = new SortField("variant.variantType.name.keyword", SortField.Type.STRING);
			}
			if (pagination.getSortBy().equalsIgnoreCase("molecularConsequence")) {
				sortField[0] = new SortField("consequence.vepConsequences.name.keyword", SortField.Type.STRING);
			}
			if (pagination.getSortBy().equalsIgnoreCase("VARIANT")) {
				sortField[0] = new SortField("allele.alleleSymbol.displayText.sort", SortField.Type.STRING);
			}
			if (pagination.getSortBy().equalsIgnoreCase("transcript")) {
				sortField[0] = new SortField("consequence.variantTranscript.name.keyword", SortField.Type.STRING);
			}
			if (pagination.getSortBy().equalsIgnoreCase("VariantHgvsName") || pagination.getSortBy().equalsIgnoreCase("symbol")) {
				sortField[0] = new SortField("variant.curatedVariantGenomicLocations.hgvs.sort", SortField.Type.STRING);
			}
		} else {
			sortField[0] = new SortField("alterationTypeSortOrder", SortField.Type.INT);
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
		if (searchResponse != null && searchResponse.getAggregations() != null) {
			extractAggregation(searchResponse, aggregations, "variantType", "filter.variantType");
			extractAggregation(searchResponse, aggregations, "hasDisease", "filter.hasDisease");
			extractAggregation(searchResponse, aggregations, "hasPhenotype", "filter.hasPhenotype");
			extractAggregation(searchResponse, aggregations, "molecularConsequences", "filter.molecularConsequence");
			extractAggregation(searchResponse, aggregations, "alterationType", "filter.alleleCategory");
			extractAggregation(searchResponse, aggregations, "impact", "filter.variantImpact");
			extractAggregation(searchResponse, aggregations, "polyphenPrediction", "filter.variantPolyphen");
			extractAggregation(searchResponse, aggregations, "siftPrediction", "filter.variantSift");
			extractAggregation(searchResponse, aggregations, "sequenceFeatureType", "filter.sequenceFeatureType");
			extractAggregation(searchResponse, aggregations, "associatedGene", "filter.associatedGeneSymbol");
		}
		return aggregations;
	}

	private void extractAggregation(SearchResponse searchResponse, Map<String, List<String>> aggregations, String aggName, String filterKey) {
		Terms terms = searchResponse.getAggregations().get(aggName);
		aggregations.put(filterKey, new ArrayList<>());
		if (terms != null) {
			for (Terms.Bucket b : terms.getBuckets()) {
				if (!b.getKey().toString().equals("")) {
					aggregations.get(filterKey).add(b.getKeyAsString());
				}
			}
			Collections.sort(aggregations.get(filterKey), String.CASE_INSENSITIVE_ORDER);
		}
	}

	public BoolQueryBuilder buildBoolQuery(String geneId, Pagination pagination) {
		BoolQueryBuilder queryBuilder = new BoolQueryBuilder();
		queryBuilder.filter(QueryBuilders.termQuery("geneIds", geneId));
		queryBuilder.filter(QueryBuilders.termQuery("category.keyword", "sequence_summary"));
		if (pagination != null) {
			Map<FieldFilter, String> filterValueMap = pagination.getFieldFilterValueMap();

			if (filterValueMap != null) {
				for (Map.Entry<FieldFilter, String> e : filterValueMap.entrySet()) {
					FieldFilter key = e.getKey();
					String value = e.getValue();
					switch (key) {
						case SYMBOL:
							queryBuilder.must(QueryBuilders.wildcardQuery("allele.alleleSymbol.displayText", "*" + value.toLowerCase() + "*"));
							break;
						case SYNONYMS:
							queryBuilder.must(QueryBuilders.wildcardQuery("allele.alleleSynonyms.displayText", "*" + value.toLowerCase() + "*"));
							break;
						case ALLELE_CATEGORY:
							queryBuilder.filter(QueryBuilders.termsQuery("alterationType.keyword", value.split("\\|")));
							break;
						case VARIANT_TYPE:
							queryBuilder.filter(QueryBuilders.termsQuery("variant.variantType.name.keyword", value.split("\\|")));
							break;
						case HAS_DISEASE:
							queryBuilder.filter(QueryBuilders.termsQuery("hasDisease", value.split("\\|")));
							break;
						case HAS_PHENOTYPE:
							queryBuilder.filter(QueryBuilders.termsQuery("hasPhenotype", value.split("\\|")));
							break;
						case MOLECULAR_CONSEQUENCE:
							queryBuilder.filter(QueryBuilders.termsQuery("consequence.vepConsequences.name.keyword", value.split("\\|")));
							break;
						case VARIANT_IMPACT:
							queryBuilder.filter(QueryBuilders.termsQuery("consequence.vepImpact.name.keyword", value.split("\\|")));
							break;
						case VARIANT_SIFT:
							queryBuilder.filter(QueryBuilders.termsQuery("consequence.siftPrediction.name.keyword", value.split("\\|")));
							break;
						case VARIANT_POLYPHEN:
							queryBuilder.filter(QueryBuilders.termsQuery("consequence.polyphenPrediction.name.keyword", value.split("\\|")));
							break;
						case SEQUENCE_FEATURE:
							queryBuilder.must(QueryBuilders.wildcardQuery("consequence.variantTranscript.name", "*" + value.toLowerCase() + "*"));
							break;
						case SEQUENCE_FEATURE_TYPE:
							queryBuilder.filter(QueryBuilders.termsQuery("consequence.variantTranscript.transcriptType.name.keyword", value.split("\\|")));
							break;
						case ASSOCIATED_GENE:
							queryBuilder.filter(QueryBuilders.termsQuery("consequence.variantTranscript.transcriptGeneAssociations.transcriptGeneAssociationObject.geneSymbol.displayText.keyword", value.split("\\|")));
							break;
						case VARIANT_HGVS_G:
							queryBuilder.must(QueryBuilders.wildcardQuery("variant.curatedVariantGenomicLocations.hgvs", "*" + value.toLowerCase() + "*"));
							break;
						case VARIANT_LOCATION:
							queryBuilder.must(QueryBuilders.wildcardQuery("consequence.intronExonLocation", "*" + value.toLowerCase() + "*"));
							break;
						default:
							break;
					}
				}
			}
		}
		return queryBuilder;
	}


	public void buildAggregations(SearchSourceBuilder srb) {
		srb.aggregation(AggregationBuilders.terms("variantType").field("variant.variantType.name.keyword"));
		srb.aggregation(AggregationBuilders.terms("hasDisease").field("hasDisease"));
		srb.aggregation(AggregationBuilders.terms("hasPhenotype").field("hasPhenotype"));
		srb.aggregation(AggregationBuilders.terms("impact").field("consequence.vepImpact.name.keyword"));
		srb.aggregation(AggregationBuilders.terms("polyphenPrediction").field("consequence.polyphenPrediction.name.keyword"));
		srb.aggregation(AggregationBuilders.terms("siftPrediction").field("consequence.siftPrediction.name.keyword"));
		srb.aggregation(AggregationBuilders.terms("sequenceFeatureType").field("consequence.variantTranscript.transcriptType.name.keyword"));
		srb.aggregation(AggregationBuilders.terms("associatedGene").field("consequence.variantTranscript.transcriptGeneAssociations.transcriptGeneAssociationObject.geneSymbol.displayText.keyword"));
		srb.aggregation(AggregationBuilders.terms("molecularConsequences").field("consequence.vepConsequences.name.keyword"));
		srb.aggregation(AggregationBuilders.terms("alterationType").field("alterationType.keyword"));
	}

}
