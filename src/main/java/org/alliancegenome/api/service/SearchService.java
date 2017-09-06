package org.alliancegenome.api.service;

import org.alliancegenome.api.dao.SearchDAO;
import org.alliancegenome.api.model.SearchResult;
import org.alliancegenome.api.service.helper.SearchHelper;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.jboss.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.util.List;

import static org.elasticsearch.index.query.QueryBuilders.*;

@RequestScoped
public class SearchService {

	@Inject
	private SearchDAO searchDAO;

	@Inject
	private SearchHelper searchHelper;

	private static Logger log = Logger.getLogger(SearchService.class);

	public SearchResult query(String q, String category, int limit, int offset, String sort_by, UriInfo uriInfo) {

		SearchResult result = new SearchResult();

		QueryBuilder query = buildQuery(q, category, getFilters(category, uriInfo));
		List<AggregationBuilder> aggBuilders = searchHelper.createAggBuilder(category);
		
		HighlightBuilder hlb = searchHelper.buildHighlights();

		SearchResponse searchResponse = searchDAO.performQuery(query, aggBuilders, limit, offset, hlb, sort_by);

		log.info("Search Query: " + q);

		result.total = searchResponse.getHits().totalHits;
		result.results = searchHelper.formatResults(searchResponse, tokenizeQuery(q));
		result.aggregations = searchHelper.formatAggResults(category, searchResponse);

		return result;
	}

	public QueryBuilder buildQuery(String q, String category, MultivaluedMap<String,String> filters) {

		BoolQueryBuilder bool = boolQuery();

		//handle the query input, if necessary
		if (StringUtils.isNotEmpty(q)) {

			MultiMatchQueryBuilder multi = multiMatchQuery(q);

			//add the fields one at a time
			searchHelper.getSearchFields().stream().forEach(multi::field);

			//this applies individual boosts, if they're in the map
			multi.fields(searchHelper.getBoostMap());

			bool.must(multi);

			//add a 'should' clause for each individual term

			//naive tokenizing, should be replaced with something smarter
			List<String> tokens = tokenizeQuery(q);
			for (String token : tokens) {
				MultiMatchQueryBuilder mmq = multiMatchQuery(token);
				searchHelper.getSearchFields().stream().forEach(mmq::field);
				mmq.fields(searchHelper.getBoostMap());
				mmq.queryName(token);
				bool.should(mmq);
			}


		} else {
			bool.must(matchAllQuery());
		}

		//apply filters if a category has been set
		if (StringUtils.isNotEmpty(category)) {
			bool.filter(new TermQueryBuilder("category", category));

			//expand the map of lists and add each key,value pair as filters
			filters.entrySet().stream().forEach(entry ->
				entry.getValue().stream().forEach( value ->
						bool.filter(new TermQueryBuilder(entry.getKey() + ".raw", value))
				)
			);

		}

		return bool;
	}

	public MultivaluedMap<String,String> getFilters(String category, UriInfo uriInfo) {
		MultivaluedMap<String,String> map = new MultivaluedHashMap<>();
		uriInfo.getQueryParameters().entrySet()
				.stream()
				.filter(entry -> searchHelper.filterIsValid(category, entry.getKey()))
				.forEach(entry -> map.addAll(entry.getKey(), entry.getValue()));
		return map;
	}

	public List<String> tokenizeQuery(String query) {
		return searchDAO.analyze(query);
	}

}
