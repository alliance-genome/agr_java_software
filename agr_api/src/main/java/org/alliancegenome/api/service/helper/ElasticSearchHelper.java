package org.alliancegenome.api.service.helper;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.alliancegenome.api.es.dao.SearchDAO;
import org.alliancegenome.api.es.query.Pagination;
import org.apache.commons.collections.MapUtils;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;

public class ElasticSearchHelper {

	private static final SearchDAO searchDAO = new SearchDAO();
	
	public void addTableFilter(Pagination pagination, BoolQueryBuilder bool) {
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
	
	public BoolQueryBuilder getBooleanAndedQueryBuilder(String filterName, String filterValue) {
		return getBooleanAndedQueryBuilder(filterName, filterValue, false);
	}

	/*
	 * split filter values by white spaces and create and ANDed boolean query
	 */
	public BoolQueryBuilder getBooleanAndedQueryBuilder(String filterName, String filterValue, boolean exactMatchOnly) {
		BoolQueryBuilder andClause = boolQuery();
		String[] elements = escapeValue(filterValue).split(" ");
		String searchStringBoundary = exactMatchOnly ? "" : "*";
		Arrays.stream(elements).forEach(element -> andClause.must(QueryBuilders.queryStringQuery(searchStringBoundary + element + searchStringBoundary).field(filterName)));
		return andClause;
	}

	private String escapeValue(String value) {
		value = value.replaceAll("'", " ");
		value = QueryParser.escape(value);
		return value;
	}

	public SearchResponse getSearchResponse(BoolQueryBuilder bool, Pagination pagination, LinkedHashMap<String, SortOrder> focusTaxonId, boolean debug) {
		List<AggregationBuilder> aggBuilders = new ArrayList<>();
		HighlightBuilder hlb = new HighlightBuilder();

		return searchDAO.performQuery(
			bool, aggBuilders, null, List.of("*"),
			pagination.getLimit(), pagination.getOffset(), hlb, focusTaxonId, debug);
	}
}
