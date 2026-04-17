package org.alliancegenome.api.service;

import static org.alliancegenome.cache.repository.helper.JsonResultResponse.DISTINCT_FIELD_VALUES;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.alliancegenome.api.entity.DiseaseRibbonSummary;
import org.alliancegenome.api.service.helper.GeneDiseaseSearchHelper;
import org.alliancegenome.es.index.site.dao.SearchDAO;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.DOTerm;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilders;
import org.elasticsearch.search.aggregations.bucket.MultiBucketsAggregation;
import org.elasticsearch.search.aggregations.bucket.terms.ParsedStringTerms;
import org.elasticsearch.search.aggregations.bucket.terms.TermsAggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;

import com.fasterxml.jackson.databind.ObjectMapper;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;


@RequestScoped
public class ESService {

	@Inject
	ObjectMapper mapper;

	private static final SearchDAO searchDAO = new SearchDAO();
	private static final GeneDiseaseSearchHelper geneDiseaseSearchHelper = new GeneDiseaseSearchHelper();


	protected SearchResponse getSearchResponse(BoolQueryBuilder bool, Pagination pagination, LinkedHashMap<String, SortOrder> focusTaxonId, boolean debug) {
		List<AggregationBuilder> aggBuilders = new ArrayList<>();
		HighlightBuilder hlb = new HighlightBuilder();

		return searchDAO.performQuery(
			bool, aggBuilders, null, geneDiseaseSearchHelper.getResponseFields(),
			pagination.getLimit(), pagination.getOffset(), hlb, focusTaxonId, debug);
	}

	protected SearchResponse getSearchResponse(BoolQueryBuilder bool, Pagination pagination, LinkedHashMap<String, SortOrder> focusTaxonId, Map<String, Boolean> missingFieldLast, boolean debug) {
		List<AggregationBuilder> aggBuilders = new ArrayList<>();
		HighlightBuilder hlb = new HighlightBuilder();

		return searchDAO.performQuery(
			bool, aggBuilders, null, geneDiseaseSearchHelper.getResponseFields(),
			pagination.getLimit(), pagination.getOffset(), hlb, focusTaxonId, missingFieldLast, debug);
	}

	BoolQueryBuilder getBaseQuery(List<String> entityIDs, String termID, boolean excludeNegated, String recordType, boolean excludeViaOrthologyRecords) {
		BoolQueryBuilder bool = boolQuery();
		BoolQueryBuilder bool2 = boolQuery();
		bool.must(bool2);

		bool.filter(termQuery("category", recordType));

		if (CollectionUtils.isNotEmpty(entityIDs)) {
			for (String geneId : entityIDs) {
				setEntityIdMatcher(geneId, bool2);
			}
		}
		if (excludeNegated) {
			bool.must(matchQuery("primaryAnnotations.negated", false));
		}
		if (excludeViaOrthologyRecords) {
			bool.must(matchQuery("viaOrthologyOrder", 0));
		}
		if (termID != null) {
			BoolQueryBuilder bool3 = boolQuery();
			bool.must(bool3);
			if (termID.equals(DiseaseRibbonSummary.DOID_OTHER)) {
				BoolQueryBuilder orClause = boolQuery();
				DOTerm.getAllOtherDiseaseTerms().forEach(parentID -> orClause.should(QueryBuilders.termQuery("parentSlimIDs.keyword", parentID)));
				bool3.should(orClause);

			} else {
				bool3.should(new MatchQueryBuilder("parentSlimIDs.keyword", termID));
			}
		}
		return bool;
	}

	BoolQueryBuilder getBaseModelQuery(List<String> entityIDs, boolean excludeNegated, String recordType) {
		BoolQueryBuilder bool = boolQuery();
		BoolQueryBuilder bool2 = boolQuery();
		bool.must(bool2);

		bool.filter(termQuery("category", recordType));

		if (CollectionUtils.isNotEmpty(entityIDs)) {
			for (String geneId : entityIDs) {
				bool2.should(new MatchQueryBuilder("gene.primaryExternalId.keyword", geneId));
			}
		}
		return bool;
	}

	void addTableFilter(Pagination pagination, BoolQueryBuilder bool) {
		HashMap<String, String> filterOptionMap = pagination.getFilterOptionMap();
		if (MapUtils.isNotEmpty(filterOptionMap)) {
			filterOptionMap.forEach((filterName, filterValue) -> generateFilter(bool, filterName, filterValue));
		}
	}

	private void generateFilter(BoolQueryBuilder bool, String filterName, String filterValue) {
		if (filterValue.contains("|")) {
			//Log.info("Or Filter: " + filterName + " " + filterValue);
			BoolQueryBuilder orClause = boolQuery();
			String[] elements = filterValue.split("\\|");
			Arrays.stream(elements).forEach(element -> orClause.should(QueryBuilders.termQuery(filterName, element)));
			bool.must(orClause);
		} else {
			//Log.info("Other Filter: " + filterName + " " + filterValue);
			if (filterName.endsWith("keyword")) {
				bool.must(QueryBuilders.termQuery(filterName, filterValue));
			} else if (isBooleanField(filterName)) {
				// Handle boolean fields with term queries instead of wildcard queries
				bool.must(QueryBuilders.termQuery(filterName, Boolean.parseBoolean(filterValue)));
			} else {
				if (filterName.contains("OR")) {
					BoolQueryBuilder outerAndClause = boolQuery();
					String[] filterNames = filterName.split("OR");
					Arrays.stream(filterNames).forEach(indivFilterName -> {
						BoolQueryBuilder orClause = getBooleanAndedQueryBuilder(indivFilterName.trim(), filterValue);
						outerAndClause.should(orClause);
					});
					bool.must(outerAndClause);
				} else {
					BoolQueryBuilder andClause = getBooleanAndedQueryBuilder(filterName, filterValue);
					bool.must(andClause);
				}
			}
		}
	}

	private boolean isBooleanField(String filterName) {
		// List of known boolean fields that should use term queries instead of wildcard queries
		return filterName.equals("alleleDocument.hasPhenotypeAnnotations")
			|| filterName.equals("alleleDocument.hasDiseaseAnnotations")
			|| filterName.equals("alleleDocument.hasVariants")
			|| filterName.equals("alleleDocument.hasConstruct")
			|| filterName.equals("primaryAnnotations.negated")
			|| filterName.endsWith(".negated")
			|| filterName.contains("hasPhenotype")
			|| filterName.contains("hasDisease")
			|| filterName.contains("hasVariant")
			|| filterName.contains("hasConstruct");
	}

	/*
	 * split filter values by white spaces and create and ANDed boolean query
	 */
	private BoolQueryBuilder getBooleanAndedQueryBuilder(String filterName, String filterValue) {
		BoolQueryBuilder andClause = boolQuery();
		String[] elements = escapeValue(filterValue).split(" ");
		// remove empty strings
		List<String> elementStrings = Stream.of(elements).filter(s -> !s.trim().isEmpty()).toList();
		elementStrings.forEach(element -> andClause.must(QueryBuilders.queryStringQuery("*" + element + "*").field(filterName)));
		return andClause;
	}

	private String escapeValue(String value) {
		value = value.replaceAll("'", " ");
		value = QueryParser.escape(value);
		return value;
	}

	protected Map<String, List<String>> getAggregations(BoolQueryBuilder bool, Map<String, String> aggregationFields, String focusTaxonId, boolean useSpeciesAggregation, boolean debug) {
		List<AggregationBuilder> aggBuilders = new ArrayList<>();
		aggregationFields.forEach((field, colName) -> {
			String fieldNameAgg = field + "_agg";
			TermsAggregationBuilder aggregationBuilder = AggregationBuilders.terms(fieldNameAgg);
			aggregationBuilder.bucketCardinality();
			aggregationBuilder.field(field);
			aggregationBuilder.size(40);
			aggBuilders.add(aggregationBuilder);
		});

		SearchResponse searchResponseHistogram = searchDAO.performQuery(
			bool, aggBuilders, null, geneDiseaseSearchHelper.getResponseFields(),
			0, 0, new HighlightBuilder(), useSpeciesAggregation ? getAnnotationSorts(focusTaxonId, debug) : null, debug);

		Map<String, List<String>> distinctFieldValueMap = new HashMap<>();
		aggregationFields.forEach((field, colName) -> {
			String fieldNameAgg = field + "_agg";
			List<String> values = ((ParsedStringTerms) searchResponseHistogram.getAggregations().get(fieldNameAgg)).getBuckets().stream()
				.map(MultiBucketsAggregation.Bucket::getKeyAsString).collect(Collectors.toList());
			distinctFieldValueMap.put(colName, values);
		});
		return distinctFieldValueMap;
	}

	protected Map<String, Object> getSupplementalData(String focusTaxonId, boolean useSpeciesAggregation, boolean debug, BoolQueryBuilder unfilteredQuery, Map<String, String> aggregationFields) {
		Map<String, List<String>> distinctFieldValueMap = getAggregations(unfilteredQuery, aggregationFields, focusTaxonId, useSpeciesAggregation, debug);
		Map<String, Object> supplementalData = new LinkedHashMap<>();
		supplementalData.put(DISTINCT_FIELD_VALUES, distinctFieldValueMap);
		return supplementalData;
	}


	protected LinkedHashMap<String, SortOrder> getAnnotationSorts(String focusTaxonId, boolean debug) {
		SpeciesType type = SpeciesType.getTypeByID(focusTaxonId);
		LinkedHashMap<String, SortOrder> sorts = new LinkedHashMap<>();
		if (type != null) {
			sorts.put("speciesOrder." + type.getTaxonIDPart(), SortOrder.ASC);
		} else {
			if (debug) {
				Log.info("Species could not be found for: " + focusTaxonId);
			} else {
				Log.debug("Species could not be found for: " + focusTaxonId);
			}
		}
		sorts.put("object.name.sort", SortOrder.ASC);
		if (debug) {
			Log.info(sorts);
		}
		return sorts;
	}

	protected void setEntityIdMatcher(String geneID, BoolQueryBuilder bool2) {
		bool2.should(new MatchQueryBuilder("subject.curie.keyword", geneID));
		bool2.should(new MatchQueryBuilder("subject.primaryExternalId.keyword", geneID));
		bool2.should(new MatchQueryBuilder("subject.modInternalId.keyword", geneID));
	}
}
