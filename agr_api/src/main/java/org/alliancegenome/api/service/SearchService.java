package org.alliancegenome.api.service;

import org.alliancegenome.api.service.helper.SearchHelper;
import org.alliancegenome.es.index.site.dao.SearchDAO;
import org.alliancegenome.es.model.search.RelatedDataLink;
import org.alliancegenome.es.model.search.SearchApiResponse;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.functionscore.FunctionScoreQueryBuilder;
import org.elasticsearch.index.query.functionscore.ScoreFunctionBuilders;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.jboss.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.elasticsearch.index.query.QueryBuilders.*;

@RequestScoped
public class SearchService {

    private SearchDAO searchDAO = new SearchDAO();

    private SearchHelper searchHelper = new SearchHelper();

    private QueryManipulationService queryManipulationService = new QueryManipulationService();

    private static Logger log = Logger.getLogger(SearchService.class);

    public SearchApiResponse query(String q, String category, int limit, int offset, String sort_by, UriInfo uriInfo) {

        SearchApiResponse result = new SearchApiResponse();

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
        result.results = searchHelper.formatResults(searchResponse, tokenizeQuery(q));
        //still too slow to leave on
        addRelatedDataLinks(result.results);
        result.aggregations = searchHelper.formatAggResults(category, searchResponse);

        return result;
    }

    public QueryBuilder buildFunctionQuery(String q, String category, MultivaluedMap<String,String> filters) {

        BoolQueryBuilder bool = buildQuery(q, category, filters);

        if (StringUtils.isEmpty(q)) {
            return bool;
        }

        FunctionScoreQueryBuilder builder = new FunctionScoreQueryBuilder(bool, buildBoostFunctions(q));

        return builder;
    }

    public FunctionScoreQueryBuilder.FilterFunctionBuilder[] buildBoostFunctions(String q) {
        List<FunctionScoreQueryBuilder.FilterFunctionBuilder> functionList = new ArrayList<>();

        //gene category boost
        functionList.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("category","gene"),
                ScoreFunctionBuilders.weightFactorFunction(1.1F)));

        functionList.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("name_key.keyword",q),
                ScoreFunctionBuilders.weightFactorFunction(1000F)));

        functionList.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("name_key.keywordAutocomplete",q),
                ScoreFunctionBuilders.weightFactorFunction(500F)));

        //per term boost, add a 'should' clause for each individual term
        List<String> tokens = tokenizeQuery(q);
        for (String token : tokens) {
            MultiMatchQueryBuilder mmq = multiMatchQuery(token);
            searchHelper.getSearchFields().stream().forEach(mmq::field);
            mmq.fields(searchHelper.getBoostMap());
            mmq.queryName(token);
            functionList.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(mmq, ScoreFunctionBuilders.weightFactorFunction(10.0F)));
        }


        return functionList.toArray(new FunctionScoreQueryBuilder.FilterFunctionBuilder[functionList.size()]);

    }



    public BoolQueryBuilder buildQuery(String q, String category, MultivaluedMap<String,String> filters) {

        BoolQueryBuilder bool = boolQuery();

        //handle the query input, if necessary
        if (StringUtils.isNotEmpty(q)) {

            QueryStringQueryBuilder builder = queryStringQuery(q)
                .defaultOperator(Operator.OR)
                .allowLeadingWildcard(true);

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
        bool.filter(searchHelper.limitCategories());

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
        List<String> tokens = new ArrayList<>();

        if (StringUtils.isEmpty(query)) {
            return tokens;
        }


        //undo colon escaping
        query = query.replaceAll("\\\\:",":");

        //normalize the whitespace
        query = query.replaceAll("\\s+", " ");

        //extract quoted phrases
        Pattern p = Pattern.compile( "\"([^\"]*)\"" );
        Matcher m = p.matcher(query);
        while( m.find()) {
            String phrase = m.group(1);
            tokens.add(phrase);
            query = query.replaceAll("\"" + phrase + "\"","");
        }

        //normalize the whitespace again
        query = query.replaceAll("\\s+", " ");

        //add the tokens
        tokens.addAll(Arrays.asList(query.split("\\s")));

        //strip boolean tokens
        List<String> booleans = new ArrayList<>();
        booleans.add("AND");
        booleans.add("OR");
        booleans.add("NOT");

        tokens.removeAll(booleans);

        return tokens;


    }

    public void addRelatedDataLinks(List<Map<String,Object>> results) {
        results.stream().forEach(x -> addRelatedDataLinks(x));
    }

    public void addRelatedDataLinks(Map<String,Object> result) {
        String nameKey = (String) result.get("name_key");
        String category = (String) result.get("category");

        List<RelatedDataLink> links = new ArrayList<>();

        if (StringUtils.equals(category,"gene")) {
            links.add(getRelatedDataLink("disease", "annotations.geneDocument.name_key", nameKey));
            links.add(getRelatedDataLink("allele", "geneDocument.name_key", nameKey));
            links.add(getRelatedDataLink("go", "go_genes", nameKey));
        } else if (StringUtils.equals(category,"disease")) {
            links.add(getRelatedDataLink("gene", "diseasesViaExperiment.name", nameKey));
            links.add(getRelatedDataLink("allele", "diseaseDocuments.name", nameKey));
        } else if (StringUtils.equals(category, "allele")) {
            links.add(getRelatedDataLink("gene", "alleles.name", nameKey));
        } else if (StringUtils.equals(category,"go")) {
            String goType = (String) result.get("go_type");
            if (StringUtils.equals(goType, "biological_process")) {
                links.add(getRelatedDataLink("gene", "biologicalProcessWithParents", nameKey));
            } else if (StringUtils.equals(goType, "molecular_function")) {
                links.add(getRelatedDataLink("gene", "molecularFunctionWithParents", nameKey));
            } else if (StringUtils.equals(goType, "cellular_component")) {
                links.add(getRelatedDataLink("gene", "cellularComponentWithParents", nameKey));
                links.add(getRelatedDataLink("gene", "cellularComponentExpressionWithParents", nameKey, "Gene via Expression"));
            }
            // need to handle the possible different fields, maybe link to more than one for CC terms
        }

        //only keep the non-zero links
        result.put("relatedData",links.stream().filter(r -> r.getCount() > 0).collect(Collectors.toList()));
    }

    public RelatedDataLink getRelatedDataLink(String targetCategory, String targetField, String sourceName) {
        return getRelatedDataLink(targetCategory, targetField, sourceName, null);
    }

    public RelatedDataLink getRelatedDataLink(String targetCategory, String targetField, String sourceName, String label) {
        MultivaluedMap<String,String> filters = new MultivaluedHashMap<>();

        filters.add(targetField, sourceName);

        Long count = searchDAO.performCountQuery(buildQuery(null, targetCategory, filters));

        RelatedDataLink relatedDataLink = new RelatedDataLink();
        relatedDataLink.setCategory(targetCategory);
        relatedDataLink.setTargetField(targetField);
        relatedDataLink.setSourceName(sourceName);
        relatedDataLink.setCount(count);
        relatedDataLink.setLabel(label);

        return relatedDataLink;
    }

}
