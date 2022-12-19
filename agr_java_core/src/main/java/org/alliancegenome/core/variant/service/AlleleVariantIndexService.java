package org.alliancegenome.core.variant.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;

import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.cache.repository.AlleleCacheRepository;
import org.alliancegenome.cache.repository.helper.AlleleFiltering;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.api.service.AlleleColumnFieldMapping;
import org.alliancegenome.core.api.service.ColumnFieldMapping;
import org.alliancegenome.core.api.service.FilterService;
import org.alliancegenome.core.api.service.Table;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.es.util.EsClientFactory;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.CrossReference;
import org.alliancegenome.neo4j.entity.node.TranscriptLevelConsequence;
import org.alliancegenome.neo4j.entity.node.Variant;
import org.apache.lucene.search.SortField;
import org.elasticsearch.action.search.ClearScrollRequest;
import org.elasticsearch.action.search.ClearScrollResponse;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchScrollRequest;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.terms.Terms;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.FieldSortBuilder;
import org.elasticsearch.search.sort.SortOrder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.jbosslog.JBossLog;

@JBossLog
@RequestScoped
public class AlleleVariantIndexService {

	private ObjectMapper mapper = new ObjectMapper();
	AlleleCacheRepository alleleCacheRepository = new AlleleCacheRepository();

	public AlleleVariantIndexService() {
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.configure(DeserializationFeature.ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT, true);
		mapper.configure(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES, false);
		mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
	}

	/** DETAIL PAGE */
	public List<AlleleVariantSequence> getAllelesNVariants(String geneId, Pagination pagination) {
		SearchResponse searchResponse = null;
		try {
			log.debug("BEFORE QUERY:" + new Date());

			searchResponse = getSearchResponse(geneId);
			log.debug(searchResponse.getHits().getHits().length);
			log.debug("AFTER QUERY:" + new Date() + "\tTOOK:" + searchResponse.getTook());

			// searchHits = getSearchResponse(geneId,pagination);
		} catch (IOException e) {
			e.printStackTrace();
		}
		List<AlleleVariantSequence> avsList = new ArrayList<>();
		if (searchResponse != null)
			for (SearchHit searchHit : searchResponse.getHits().getHits()) {
				AlleleVariantSequence avsDocument = null;
				Allele allele = null;
				try {
					avsDocument = mapper.readValue(searchHit.getSourceAsString(), AlleleVariantSequence.class);
					allele = avsDocument.getAllele();
				} catch (JsonProcessingException e) {
					e.printStackTrace();
				}
				if (avsDocument != null && avsDocument.getAlterationType().equalsIgnoreCase("variant")) {
					allele.setCategory("variant");

				}

				if (allele != null) {
					if (allele.getModCrossRefCompleteUrl() == null) {
						allele.setModCrossRefCompleteUrl(" ");
					}
					if (allele.getId() == null || (allele.getId() != null && allele.getId().equals("null"))) {
						allele.setId(0L);

					}
					if (allele.getVariants() != null)
						for (Variant variant : allele.getVariants()) {
							if (variant.getTranscriptLevelConsequence() != null && variant.getTranscriptLevelConsequence().size() > 0) {
								for (TranscriptLevelConsequence consequence : variant.getTranscriptLevelConsequence()) {
									AlleleVariantSequence seq = new AlleleVariantSequence(allele, variant, consequence);
									avsList.add(seq);
								}
							} else {
								AlleleVariantSequence seq = new AlleleVariantSequence(allele, variant, null);
								avsList.add(seq);
							}
						}
					else {
						AlleleVariantSequence seq = new AlleleVariantSequence(allele, null, null);
						avsList.add(seq);
					}
				}
			}

		log.debug("TOTAL HITS:" + searchResponse.getHits().getTotalHits().value);
		log.debug("Allele Variant Sequences:" + avsList.size());
		return avsList;
	}

	/* GENE PAGE */
	public JsonResultResponse<Allele> getAlleles(String geneId, Pagination pagination) {

		try {
			return getSearchResponse(geneId, pagination);
		} catch (IOException e) {
			e.printStackTrace();
		}

		return new JsonResultResponse<>();

	}

	public JsonResultResponse<Allele> getJsonResult(List<SearchHit> searchHits, Pagination pagination, String id) {

		List<Allele> alleles = new ArrayList<>();
		for (SearchHit searchHit : searchHits) {
			AlleleVariantSequence avsDocument = null;
			Allele allele = null;
			try {
				avsDocument = mapper.readValue(searchHit.getSourceAsString(), AlleleVariantSequence.class);
				allele = avsDocument.getAllele();
			} catch (JsonProcessingException e) {
				e.printStackTrace();
			}
			if (allele != null) {
				if (avsDocument.getAlterationType().equalsIgnoreCase("variant")) {
					allele.setCategory("variant");

				}
				if (allele.getModCrossRefCompleteUrl() == null) {
					allele.setModCrossRefCompleteUrl(" ");
				}
				if (allele.getCrossReferenceMap() == null) {
					Map<String, Object> crossReferenceMap = new HashMap<>();
					CrossReference cr = new CrossReference();
					cr.setCrossRefCompleteUrl("");
					crossReferenceMap.put("primary", cr);
					allele.setCrossReferenceMap(crossReferenceMap);
				}
				alleles.add(allele);
			}

		}

		log.debug("TOTAL HITS:" + searchHits.size());
		log.debug("Alleles :" + alleles.size());

		JsonResultResponse<Allele> response = new JsonResultResponse<>();

		if (pagination.getLimit() + pagination.getPage() > 150000) {
			FilterService<Allele> filterService = new FilterService<>(new AlleleFiltering());
			ColumnFieldMapping<Allele> mapping = new AlleleColumnFieldMapping();
			List<Allele> filteredAlleleList = filterService.filterAnnotations(alleles, pagination.getFieldFilterValueMap());
			response.setResults(alleleCacheRepository.getSortedAndPaginatedAlleles(filteredAlleleList, pagination));
			response.setTotal(filteredAlleleList.size());
			// add distinct values
			response.addDistinctFieldValueSupplementalData(filterService.getDistinctFieldValues(alleles, mapping.getSingleValuedFieldColumns(Table.ALLELE_GENE), mapping));

		} else {
			response.setResults(alleles);
			response.setTotal((int) pagination.getTotalHits());
			response.addDistinctFieldValueSupplementalData(getAggregations(id));
		}

		return response;
	}

	public SearchResponse getSearchResponse(String id) throws IOException {
		SearchSourceBuilder srb = new SearchSourceBuilder();
		srb.query(buildBoolQuery(id));
		srb.size(150000);
		srb.trackTotalHits(true);

		SearchRequest searchRequest = new SearchRequest(ConfigHelper.getEsIndex());

		searchRequest.source(srb);
		log.info(searchRequest);
		return EsClientFactory.getDefaultEsClient().search(searchRequest, EsClientFactory.LARGE_RESPONSE_REQUEST_OPTIONS);
	}

	public JsonResultResponse<Allele> getSearchResponse(String id, Pagination pagination) throws IOException {
		SearchRequest searchRequest = new SearchRequest(ConfigHelper.getEsIndex());
		SearchResponse searchResponse = null;
		List<SearchHit> searchHits = new ArrayList<>();

		SearchSourceBuilder srb = new SearchSourceBuilder();
		int from = 0;
		if (pagination.getPage() > 1) {
			from = pagination.getLimit() * (pagination.getPage() - 1);
		}
		srb.query(buildBoolQuery(id, pagination));
		srb.sort(new FieldSortBuilder(getSortFields(pagination)[0].getField()).order(SortOrder.ASC));
		srb.size(pagination.getLimit());
		srb.trackTotalHits(true);
		
		RestHighLevelClient searchClient = EsClientFactory.getDefaultEsClient();

		if (from + pagination.getLimit() <= 150000) {
			srb.from(from);
			searchRequest.source(srb);
			searchResponse = searchClient.search(searchRequest, EsClientFactory.LARGE_RESPONSE_REQUEST_OPTIONS);
			searchHits.addAll(Arrays.asList(searchResponse.getHits().getHits()));
			pagination.setTotalHits(searchResponse.getHits().getTotalHits().value);

		} else {
			srb.size(10000);
			searchRequest.source(srb);
			searchRequest.scroll(TimeValue.timeValueSeconds(60));
			searchResponse = searchClient.search(searchRequest, EsClientFactory.LARGE_RESPONSE_REQUEST_OPTIONS);
			String scrollId = searchResponse.getScrollId();
			searchHits.addAll(Arrays.asList(searchResponse.getHits().getHits()));
			pagination.setTotalHits(searchResponse.getHits().getTotalHits().value);

			while (searchResponse.getHits().getHits().length > 0) {
				SearchScrollRequest scrollRequest = new SearchScrollRequest(scrollId);
				scrollRequest.scroll(TimeValue.timeValueSeconds(60));
				searchResponse = searchClient.scroll(scrollRequest, EsClientFactory.LARGE_RESPONSE_REQUEST_OPTIONS);
				scrollId = searchResponse.getScrollId();
				searchHits.addAll(Arrays.asList(searchResponse.getHits().getHits()));
			}
			;
			ClearScrollRequest clearScrollRequest = new ClearScrollRequest();
			clearScrollRequest.addScrollId(scrollId);
			ClearScrollResponse clearScrollResponse = searchClient.clearScroll(clearScrollRequest, RequestOptions.DEFAULT);
		}
		return getJsonResult(searchHits, pagination, id);
	}

	public SortField[] getSortFields(Pagination pagination) {
		SortField[] sortField = new SortField[2];

		if (pagination.getSortBy() != null && !pagination.getSortBy().equalsIgnoreCase("default")) {
			if (pagination.getSortBy().equalsIgnoreCase("variantType"))
				sortField[0] = new SortField("variantType.keyword", SortField.Type.STRING);
			if (pagination.getSortBy().equalsIgnoreCase("molecularConsequence"))
				sortField[0] = new SortField("molecularConsequence.keyword", SortField.Type.STRING);
			if (pagination.getSortBy().equalsIgnoreCase("VARIANT"))
				sortField[0] = new SortField("allele.variants.displayName.keyword", SortField.Type.STRING);

			if (pagination.getSortBy().equalsIgnoreCase("transcript"))
				sortField[0] = new SortField("allele.variants.transcriptLevelConsequence.transcript.name.keyword", SortField.Type.STRING);
			if (pagination.getSortBy().equalsIgnoreCase("VariantHgvsName") || pagination.getSortBy().equalsIgnoreCase("symbol"))
				sortField[0] = new SortField("allele.variants.hgvsG.keyword", SortField.Type.STRING);
		} else {
			sortField[0] = new SortField("alterationType.keyword", SortField.Type.STRING);
			// sortField[1]=new SortField("symbol.keyword", SortField.Type.STRING);
		}
		return sortField;
	}

	public BoolQueryBuilder buildBoolQuery(String id) {
		BoolQueryBuilder queryBuilder = new BoolQueryBuilder();
		queryBuilder.must(QueryBuilders.termQuery("geneIds.keyword", id)).filter(QueryBuilders.termQuery("category.keyword", "allele"));

		return queryBuilder;
	}

	public BoolQueryBuilder buildBoolQuery(String id, Pagination pagination) {
		BoolQueryBuilder queryBuilder = new BoolQueryBuilder();
		queryBuilder.filter(QueryBuilders.termQuery("geneIds.keyword", id));
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
						if (e.getValue().toString().split("\\|").length < 3)
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
					/*
					 * FOR FUTURE PURPOSE
					 * if(e.getKey().toString().equalsIgnoreCase("VARIANT_IMPACT")){
					 * queryBuilder.filter(QueryBuilders.termsQuery(
					 * "allele.variants.transcriptLevelConsequence.impact.keyword",
					 * e.getValue().toString().split("\\|"))); }
					 * if(e.getKey().toString().equalsIgnoreCase("VARIANT_POLYPHEN")){
					 * queryBuilder.filter(QueryBuilders.termsQuery(
					 * "allele.variants.transcriptLevelConsequence.polyphenPrediction.keyword",
					 * e.getValue().toString().split("\\|"))); }
					 * if(e.getKey().toString().equalsIgnoreCase("VARIANT_SIFT")){
					 * queryBuilder.filter(QueryBuilders.termsQuery(
					 * "allele.variants.transcriptLevelConsequence.siftPrediction.keyword",
					 * e.getValue().toString().split("\\|"))); }
					 * if(e.getKey().toString().equalsIgnoreCase("SEQUENCE_FEATURE_TYPE")){
					 * queryBuilder.filter(QueryBuilders.termsQuery(
					 * "allele.variants.transcriptLevelConsequence.sequenceFeatureType.keyword",
					 * e.getValue().toString().split("\\|"))); }
					 * if(e.getKey().toString().equalsIgnoreCase("SEQUENCE_FEATURE")){
					 * queryBuilder.filter(QueryBuilders.termsQuery(
					 * "allele.variants.transcriptLevelConsequence.transcript.name.keyword",
					 * e.getValue().toString().split("\\|"))); }
					 * if(e.getKey().toString().equalsIgnoreCase("ASSOCIATED_GENE")){
					 * queryBuilder.filter(QueryBuilders.termsQuery("allele.gene.symbol.keyword",
					 * e.getValue().toString().split("\\|"))); }
					 * if(e.getKey().toString().equalsIgnoreCase("VARIANT_LOCATION")){
					 * queryBuilder.filter(QueryBuilders.termsQuery(
					 * "allele.variants.transcriptLevelConsequence.location.keyword",
					 * e.getValue().toString().split("\\|"))); }
					 */
				}
			}
		}
		return queryBuilder;
	}

	public void buildAggregations(SearchSourceBuilder searchSourceBuilder) {
		List<String> aggregationFields = new ArrayList<>(Arrays.asList("variantType", "hasDisease", "allele.hasPhenotype", "allele.variants.transcriptLevelConsequence.impact",
			"allele.variants.transcriptLevelConsequence.polyphenPrediction", "allele.variants.transcriptLevelConsequence.siftPrediction",
			"allele.variants.transcriptLevelConsequence.sequenceFeatureType", "allele.variants.transcriptLevelConsequence.transcript.name", "allele.gene.symbol",
			"allele.variants.transcriptLevelConsequence.location", "allele.variants.transcriptLevelConsequence.molecularConsequences", "alterationType"));
		for (String field : aggregationFields) {
			searchSourceBuilder.aggregation(buildAggregations(field));
		}

	}

	public AggregationBuilder buildAggregations(String field) {

		AggregationBuilder aggregationBuilder = null;
		if (field.contains("impact"))
			aggregationBuilder = AggregationBuilders.terms("impact").field(field + ".keyword");
		else if (field.contains("polyphenPrediction"))
			aggregationBuilder = AggregationBuilders.terms("polyphenPrediction").field(field + ".keyword");
		else if (field.contains("siftPrediction"))
			aggregationBuilder = AggregationBuilders.terms("siftPrediction").field(field + ".keyword");
		else if (field.contains("sequenceFeatureType"))
			aggregationBuilder = AggregationBuilders.terms("sequenceFeatureType").field(field + ".keyword");
		else if (field.contains("transcript.name"))
			aggregationBuilder = AggregationBuilders.terms("transcriptName").field(field + ".keyword");
		else if (field.contains("location"))
			aggregationBuilder = AggregationBuilders.terms("location").field(field + ".keyword");
		else if (field.contains("hasPhenotype"))
			aggregationBuilder = AggregationBuilders.terms("hasPhenotype").field(field + ".keyword");
		else if (field.contains("molecularConsequences"))
			aggregationBuilder = AggregationBuilders.terms("molecularConsequences").field(field + ".keyword");
		else if (field.contains("alterationType"))
			aggregationBuilder = AggregationBuilders.terms("alterationType").field(field + ".keyword");
		else
			aggregationBuilder = AggregationBuilders.terms(field).field(field + ".keyword");

		return aggregationBuilder;
	}

	public Map<String, List<String>> getAggregations(String id) {
		SearchRequest searchRequest = new SearchRequest(ConfigHelper.getEsIndex());
		SearchResponse searchResponse = null;

		SearchSourceBuilder srb = new SearchSourceBuilder();
		srb.size(0);
		srb.query(buildBoolQuery(id, null));
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
					if (!b.getKey().toString().equals(""))
						aggregations.get("filter.variantType").add((String) b.getKey());
				}
			}
			Terms hasDiseaseAggs = searchResponse.getAggregations().get("hasDisease");
			aggregations.put("filter.hasDisease", new ArrayList<>());
			for (Terms.Bucket b : hasDiseaseAggs.getBuckets()) {
				if (!b.getKey().toString().equals(""))
					aggregations.get("filter.hasDisease").add((String) b.getKey());
			}
			Terms hasPhenotype = searchResponse.getAggregations().get("hasPhenotype");
			aggregations.put("filter.hasPhenotype", new ArrayList<>());
			for (Terms.Bucket b : hasPhenotype.getBuckets()) {
				if (!b.getKey().toString().equals(""))
					aggregations.get("filter.hasPhenotype").add((String) b.getKey());
			}
			Terms molecularConsequences = searchResponse.getAggregations().get("molecularConsequences");
			aggregations.put("filter.molecularConsequence", new ArrayList<>());
			for (Terms.Bucket b : molecularConsequences.getBuckets()) {
				if (!b.getKey().toString().equals(""))
					aggregations.get("filter.molecularConsequence").add((String) b.getKey());
			}
			/*
			 * For future purpose Terms impact =
			 * searchResponse.getAggregations().get("impact");
			 * aggregations.put("filter.impact", new ArrayList<>()); for (Terms.Bucket b :
			 * impact.getBuckets()) { if (!b.getKey().toString().equals(""))
			 * aggregations.get("filter.impact").add((String) b.getKey()); } Terms
			 * polyphenPrediction =
			 * searchResponse.getAggregations().get("polyphenPrediction");
			 * aggregations.put("filter.variantPolyphen", new ArrayList<>()); for
			 * (Terms.Bucket b : polyphenPrediction.getBuckets()) { if
			 * (!b.getKey().toString().equals(""))
			 * aggregations.get("filter.variantPolyphen").add((String) b.getKey()); } Terms
			 * siftPrediction = searchResponse.getAggregations().get("siftPrediction");
			 * aggregations.put("filter.variantSift", new ArrayList<>()); for (Terms.Bucket
			 * b : siftPrediction.getBuckets()) { if (!b.getKey().toString().equals(""))
			 * aggregations.get("filter.variantSift").add((String) b.getKey()); } Terms
			 * sequenceFeatureType =
			 * searchResponse.getAggregations().get("sequenceFeatureType");
			 * aggregations.put("filter.sequenceFeatureType", new ArrayList<>()); for
			 * (Terms.Bucket b : sequenceFeatureType.getBuckets()) { if
			 * (!b.getKey().toString().equals(""))
			 * aggregations.get("filter.sequenceFeatureType").add((String) b.getKey()); }
			 * Terms transcriptName =
			 * searchResponse.getAggregations().get("transcriptName");
			 * aggregations.put("filter.transcriptName", new ArrayList<>()); for
			 * (Terms.Bucket b : transcriptName.getBuckets()) { if
			 * (!b.getKey().toString().equals(""))
			 * aggregations.get("filter.transcriptName").add((String) b.getKey()); } Terms
			 * location = searchResponse.getAggregations().get("location");
			 * aggregations.put("filter.variantLocation", new ArrayList<>()); for
			 * (Terms.Bucket b : location.getBuckets()) { if
			 * (!b.getKey().toString().equals(""))
			 * aggregations.get("filter.variantLocation").add((String) b.getKey()); }
			 */

			Terms alterationType = searchResponse.getAggregations().get("alterationType");
			aggregations.put("filter.alleleCategory", new ArrayList<>());
			for (Terms.Bucket b : alterationType.getBuckets()) {
				if (!b.getKey().toString().equals(""))
					aggregations.get("filter.alleleCategory").add((String) b.getKey());
			}
		}
		return aggregations;
	}
}
