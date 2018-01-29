package org.alliancegenome.api.service;

import org.alliancegenome.api.dao.SearchDAO;
import org.alliancegenome.api.model.Category;
import org.alliancegenome.api.model.SearchResult;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.elasticsearch.index.query.QueryBuilders.matchAllQuery;
import static org.elasticsearch.index.query.QueryBuilders.multiMatchQuery;
import static org.elasticsearch.index.query.QueryBuilders.queryStringQuery;
import static org.elasticsearch.index.query.QueryBuilders.termQuery;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;


import org.alliancegenome.api.service.helper.SearchHelper;
import org.alliancegenome.shared.es.dao.site_index.SearchDAO;
import org.alliancegenome.shared.es.model.search.SearchResult;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.Operator;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryStringQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.jboss.logging.Logger;

@RequestScoped
public class SearchService {

    @Inject
    private SearchDAO searchDAO;

    @Inject
    private SearchHelper searchHelper;

    @Inject
    private QueryManipulationService queryManipulationService;

    private static Logger log = Logger.getLogger(SearchService.class);

    public SearchResult query(String q, String category, int limit, int offset, String sort_by, UriInfo uriInfo) {

        SearchResult result = new SearchResult();

        Boolean debug = false;
        if (StringUtils.isNotEmpty(q) && q.startsWith("debug")) {
            debug = true;
            q = q.replaceFirst("debug","").trim();
        }

        q = queryManipulationService.processQuery(q);

        QueryBuilder query = buildFunctionQuery(q, category, getFilters(category, uriInfo));

        List<AggregationBuilder> aggBuilders = searchHelper.createAggBuilder(category);

        HighlightBuilder hlb = searchHelper.buildHighlights();

        SearchResponse searchResponse = searchDAO.performQuery(query, aggBuilders, limit, offset, hlb, sort_by, debug);

        log.debug("Search Query: " + q);

        result.total = searchResponse.getHits().totalHits;
        result.results = searchHelper.formatResults(searchResponse, searchHelper.tokenizeQuery(q));
        result.aggregations = searchHelper.formatAggResults(category, searchResponse);

        return result;
    }

    public QueryBuilder buildFunctionQuery(String q, String category, MultivaluedMap<String,String> filters) {

        BoolQueryBuilder bool = buildQuery(q, category, filters);

        if (StringUtils.isEmpty(q)) {
            return bool;
        }

        List<FunctionScoreQueryBuilder.FilterFunctionBuilder> functionList = new ArrayList<>();

        //add a 'should' clause for each individual term
        List<String> tokens = searchHelper.tokenizeQuery(q);
        for (String token : tokens) {
            MultiMatchQueryBuilder mmq = multiMatchQuery(token);
            searchHelper.getSearchFields().stream().forEach(mmq::field);
            mmq.fields(searchHelper.getBoostMap());
            mmq.queryName(token);
            functionList.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(mmq, ScoreFunctionBuilders.weightFactorFunction(10.0F)));
        }

        FunctionScoreQueryBuilder builder = new FunctionScoreQueryBuilder(bool, functionList.toArray(new FunctionScoreQueryBuilder.FilterFunctionBuilder[functionList.size()]));

        return builder;
    }

    public BoolQueryBuilder buildQuery(String q, String category, MultivaluedMap<String,String> filters) {

        BoolQueryBuilder bool = boolQuery();

        //handle the query input, if necessary
        if (StringUtils.isNotEmpty(q)) {

            QueryStringQueryBuilder builder = queryStringQuery(q)
                .defaultOperator(Operator.OR)
                .allowLeadingWildcard(true)
                .autoGeneratePhraseQueries(true)
                .useDisMax(true);

            //add the fields one at a time
            searchHelper.getSearchFields().stream().forEach(builder::field);

            //this applies individual boosts, if they're in the map
            builder.fields(searchHelper.getBoostMap());

            bool.must(builder);

        } else {
            bool.must(matchAllQuery());
        }

        //apply filters if a category has been set
        if (StringUtils.isNotEmpty(category)) {
            bool.filter(new TermQueryBuilder("category", category));

            //expand the map of lists and add each key,value pair as filters
            filters.entrySet().stream().forEach(entry ->
                entry.getValue().stream().forEach( value ->
                        bool.filter(new TermQueryBuilder(entry.getKey() + ".keyword", value))
                )
            );

        }

        //include only searchable categories in search results
        bool.filter(limitCategories());

        return bool;
    }

    public BoolQueryBuilder limitCategories() {
        BoolQueryBuilder bool = boolQuery();
        Arrays.asList(Category.values()).stream()
                .filter(cat ->  cat.isSearchable() )
                .forEach(cat ->
                        bool.should(termQuery("category", cat.getName()))
                );
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


}
