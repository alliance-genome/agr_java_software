package org.alliancegenome.api.service;

import static org.alliancegenome.cache.repository.helper.JsonResultResponse.DISTINCT_FIELD_VALUES;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.alliancegenome.api.entity.GeneGeneticInteractionDocument;
import org.alliancegenome.api.entity.GeneMolecularInteractionDocument;
import org.alliancegenome.api.entity.GenePhenotypeAnnotationDocument;
import org.alliancegenome.api.service.helper.ElasticSearchHelper;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.variant.service.AlleleVariantIndexService;
import org.alliancegenome.curation_api.model.document.es.SequenceSummaryDocument;
import org.alliancegenome.es.index.site.dao.SearchDAO;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class GeneService {

	private static GeneRepository geneRepo = new GeneRepository();

	@Inject
	AlleleVariantIndexService alleleVariantIndexService;

	@Inject
	PhenotypeESService phenotypeESService;
	@Inject
	ObjectMapper mapper;

	private static final ElasticSearchHelper elasticSearchHelper = new ElasticSearchHelper();
	private static final SearchDAO searchDAO = new SearchDAO();

	public Gene getById(String id) {
		Gene gene = geneRepo.getOneGene(id);
		// if not found directly check if it is a secondary id on a different gene
		if (gene == null) {
			return geneRepo.getOneGeneBySecondaryId(id);
		}
		return gene;
	}

	public JsonResultResponse<SequenceSummaryDocument> getAllelesAndVariantInfo(String geneId, Pagination pagination) {
		return alleleVariantIndexService.getAllelesNVariants(geneId, pagination);
	}

	public JsonResultResponse<GeneGeneticInteractionDocument> getGeneticInteractions(String geneId, Pagination pagination) {
		BoolQueryBuilder query = boolQuery();

		String[] idFields = { "geneGeneticInteraction.geneAssociationSubject.curie.keyword", "geneGeneticInteraction.geneAssociationSubject.primaryExternalId.keyword" };
		BoolQueryBuilder idQuery = boolQuery();
		Arrays.stream(idFields).forEach(idField -> {
			BoolQueryBuilder orClause = elasticSearchHelper.getBooleanAndedQueryBuilder(idField, geneId, true);
			idQuery.should(orClause);
		});

		query.must(idQuery);

		JsonResultResponse<GeneGeneticInteractionDocument> ret = new JsonResultResponse<>();
		ret.setSupplementalData(getGeneticInteractionSupplementalData(query));

		// add table filter
		elasticSearchHelper.addTableFilter(pagination, query);

		LinkedHashMap<String, SortOrder> sorts = new LinkedHashMap<>();
		if (StringUtils.isNotBlank(pagination.getSortBy())) {
			sorts.put(pagination.getSortBy(), SortOrder.ASC);
		}

		List<AggregationBuilder> aggBuilders = new ArrayList<>();
		HighlightBuilder hlb = new HighlightBuilder();
		SearchResponse searchResponse = searchDAO.performQuery(query, aggBuilders, null, List.of("*"), pagination.getLimit(), pagination.getOffset(), hlb, sorts, false);
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);

		List<GeneGeneticInteractionDocument> list = Arrays.stream(searchResponse.getHits().getHits()).map(searchHit -> {
			try {
				GeneGeneticInteractionDocument object = mapper.readValue(searchHit.getSourceAsString(), GeneGeneticInteractionDocument.class);
				return object;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}).toList();
		ret.setResults(list);
		return ret;

	}

	public JsonResultResponse<GeneMolecularInteractionDocument> getMolecularInteractions(String geneId, Pagination pagination) {
		BoolQueryBuilder query = boolQuery();
		String[] idFields = { "geneMolecularInteraction.geneAssociationSubject.curie.keyword", "geneMolecularInteraction.geneAssociationSubject.primaryExternalId.keyword" };
		BoolQueryBuilder idQuery = boolQuery();
		Arrays.stream(idFields).forEach(idField -> {
			BoolQueryBuilder orClause = elasticSearchHelper.getBooleanAndedQueryBuilder(idField, geneId, true);
			idQuery.should(orClause);
		});

		query.must(idQuery);

		JsonResultResponse<GeneMolecularInteractionDocument> ret = new JsonResultResponse<>();
		ret.setSupplementalData(getMolecularInteractionSupplementalData(query));

		// add table filter
		elasticSearchHelper.addTableFilter(pagination, query);

		LinkedHashMap<String, SortOrder> sorts = new LinkedHashMap<>();
		if (StringUtils.isNotBlank(pagination.getSortBy())) {
			sorts.put(pagination.getSortBy(), SortOrder.ASC);
		}

		List<AggregationBuilder> aggBuilders = new ArrayList<>();
		HighlightBuilder hlb = new HighlightBuilder();
		SearchResponse searchResponse = searchDAO.performQuery(query, aggBuilders, null, List.of("*"), pagination.getLimit(), pagination.getOffset(), hlb, sorts, false);
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);

		List<GeneMolecularInteractionDocument> list = Arrays.stream(searchResponse.getHits().getHits()).map(searchHit -> {
			try {
				GeneMolecularInteractionDocument object = mapper.readValue(searchHit.getSourceAsString(), GeneMolecularInteractionDocument.class);
				return object;
			} catch (Exception e) {
				e.printStackTrace();
			}
			return null;
		}).toList();
		ret.setResults(list);
		return ret;

	}

	private Map<String, Object> getGeneticInteractionSupplementalData(BoolQueryBuilder unfilteredQuery) {
		Map<String, String> aggregationFields = new HashMap<>();
		aggregationFields.put("geneGeneticInteraction.interactorARole.name.keyword", "filter.role");
		aggregationFields.put("geneGeneticInteraction.interactorBRole.name.keyword", "filter.interactorRole");
		aggregationFields.put("geneGeneticInteraction.interactionType.name.keyword", "filter.interactionType");
		aggregationFields.put("geneGeneticInteraction.geneGeneAssociationObject.taxon.name.keyword", "filter.interactorSpecies");
		return getInteractionSupplementalData(aggregationFields, unfilteredQuery);
	}

	private Map<String, Object> getMolecularInteractionSupplementalData(BoolQueryBuilder unfilteredQuery) {
		Map<String, String> aggregationFields = new HashMap<>();
		aggregationFields.put("geneMolecularInteraction.interactorBType.name.keyword", "filter.interactorMoleculeType");
		aggregationFields.put("geneMolecularInteraction.interactorAType.name.keyword", "filter.moleculeType");
		aggregationFields.put("geneMolecularInteraction.detectionMethod.name.keyword", "filter.detectionMethod");
		aggregationFields.put("geneMolecularInteraction.geneGeneAssociationObject.taxon.name.keyword", "filter.interactorSpecies");
		return getInteractionSupplementalData(aggregationFields, unfilteredQuery);
	}

	private Map<String, Object> getInteractionSupplementalData(Map<String, String> aggregationFields, BoolQueryBuilder unfilteredQuery) {
		Map<String, List<String>> distinctFieldValueMap = getAggregations(unfilteredQuery, aggregationFields);
		Map<String, Object> supplementalData = new LinkedHashMap<>();
		supplementalData.put(DISTINCT_FIELD_VALUES, distinctFieldValueMap);
		return supplementalData;
	}

	private Map<String, List<String>> getAggregations(BoolQueryBuilder bool, Map<String, String> aggregationFields) {
		List<AggregationBuilder> aggBuilders = new ArrayList<>();
		aggregationFields.forEach((field, colName) -> {
			String fieldNameAgg = field + "_agg";
			TermsAggregationBuilder aggregationBuilder = AggregationBuilders.terms(fieldNameAgg);
			aggregationBuilder.bucketCardinality();
			aggregationBuilder.size(100);
			aggregationBuilder.field(field);
			aggBuilders.add(aggregationBuilder);
		});

		SearchResponse searchResponseHistogram = searchDAO.performQuery(bool, aggBuilders, null, List.of("*"), 0, 0, new HighlightBuilder(), null, false);

		Map<String, List<String>> distinctFieldValueMap = new HashMap<>();
		aggregationFields.forEach((field, colName) -> {
			String fieldNameAgg = field + "_agg";
			List<String> values = ((ParsedStringTerms) searchResponseHistogram.getAggregations().get(fieldNameAgg)).getBuckets().stream().map(MultiBucketsAggregation.Bucket::getKeyAsString).sorted().collect(Collectors.toList());
			distinctFieldValueMap.put(colName, values);
		});
		return distinctFieldValueMap;
	}

	public JsonResultResponse<GenePhenotypeAnnotationDocument> getPhenotypeAnnotations(String geneID, Pagination pagination) {
		LocalDateTime startDate = LocalDateTime.now();
		JsonResultResponse<GenePhenotypeAnnotationDocument> response = phenotypeESService.getGenePhenotypeAnnotations(geneID, pagination, false);
		response.calculateRequestDuration(startDate);
		return response;
	}

}