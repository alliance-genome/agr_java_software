package org.alliancegenome.api.service;

import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.UriInfo;

import org.alliancegenome.api.dao.SearchDAO;
import org.alliancegenome.api.model.SearchResult;
import org.alliancegenome.api.service.helper.SearchHelper;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;

@RequestScoped
public class SearchService {

	@Inject
	private SearchDAO searchDAO;

	@Inject
	private SearchHelper searchHelper;

	public SearchResult query(String q, String category, int limit, int offset, String sort_by, UriInfo uriInfo) {

		SearchResult result = new SearchResult();

		QueryBuilder query = searchHelper.buildQuery(q, category, uriInfo);
		List<AggregationBuilder> aggBuilders = searchHelper.createAggBuilder(category);
		
		HighlightBuilder hlb = searchHelper.buildHighlights();
		
		SearchResponse[] ret = searchDAO.performQuery(query, aggBuilders, limit, offset, hlb, sort_by);

		result.total = ret[0].getHits().totalHits;
		result.results = searchHelper.formatResults(ret[0]);
		result.aggregations = searchHelper.formatAggResults(category, ret[1]);

		return result;
	}
	
	
}
