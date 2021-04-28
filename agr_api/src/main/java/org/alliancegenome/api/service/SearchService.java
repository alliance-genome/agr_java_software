package org.alliancegenome.api.service;

import static org.elasticsearch.index.query.QueryBuilders.*;

import java.util.*;
import java.util.regex.*;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.core.*;

import org.alliancegenome.api.service.helper.SearchHelper;
import org.alliancegenome.es.index.site.dao.SearchDAO;
import org.alliancegenome.es.model.search.*;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.common.lucene.search.function.FieldValueFactorFunction;
import org.elasticsearch.index.query.*;
import org.elasticsearch.index.query.functionscore.*;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.rescore.QueryRescorerBuilder;
import org.jboss.logging.Logger;

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

        MultivaluedMap filterMap = getFilters(category, uriInfo);

        QueryBuilder query = buildFunctionQuery(q, category, filterMap);

        QueryRescorerBuilder rescorerBuilder = buildRescorer(q);

        List<AggregationBuilder> aggBuilders = searchHelper.createAggBuilder(category, biotypeSelected(filterMap));

        HighlightBuilder hlb = searchHelper.buildHighlights();

        SearchResponse searchResponse = searchDAO.performQuery(query, aggBuilders, rescorerBuilder, searchHelper.getResponseFields(), limit, offset, hlb, sort_by, debug);

        log.debug("Search Query: " + q);

        result.setTotal(searchResponse.getHits().getTotalHits().value);
        result.setResults(searchHelper.formatResults(searchResponse, tokenizeQuery(q)));
        //still too slow to leave on
        addRelatedDataLinks(result.getResults());
        result.setAggregations(searchHelper.formatAggResults(category, searchResponse));

        return result;
    }

    public QueryRescorerBuilder buildRescorer(String q) {
        if (StringUtils.isEmpty(q)) {
            return new QueryRescorerBuilder(new FunctionScoreQueryBuilder(buildRescoreMatchAllBoostFunctions()));
        }

        return new QueryRescorerBuilder(new FunctionScoreQueryBuilder(buildBoostFunctions(q)));
    }

    public QueryBuilder buildFunctionQuery(String q, String category, MultivaluedMap<String,String> filters) {

        BoolQueryBuilder bool = buildQuery(q, category, filters);

        if (StringUtils.isEmpty(q)) {
            return new FunctionScoreQueryBuilder(bool, buildMatchAllBoostFunctions());
        }

        FunctionScoreQueryBuilder builder = new FunctionScoreQueryBuilder(bool, buildBoostFunctions(q));

        return builder;
    }

    public FunctionScoreQueryBuilder.FilterFunctionBuilder[] buildMatchAllBoostFunctions() {
        List<FunctionScoreQueryBuilder.FilterFunctionBuilder> functionList = new ArrayList<>();

        functionList.add(documentPopularityBoost());

        return functionList.toArray(new FunctionScoreQueryBuilder.FilterFunctionBuilder[functionList.size()]);
    }

    public FunctionScoreQueryBuilder.FilterFunctionBuilder[] buildRescoreMatchAllBoostFunctions() {
        List<FunctionScoreQueryBuilder.FilterFunctionBuilder> functionList = new ArrayList<>();

        functionList.add(geneCategoryBoost());
        functionList.add(humanSpeciesBoost());
        functionList.add(documentHasDiseaseBoost());
        functionList.add(proteinCodingBoost());
        functionList.add(rnaBoost());
        functionList.add(pseudogeneBoost());

        return functionList.toArray(new FunctionScoreQueryBuilder.FilterFunctionBuilder[functionList.size()]);
    }

    public FunctionScoreQueryBuilder.FilterFunctionBuilder[] buildBoostFunctions(String q) {
        List<FunctionScoreQueryBuilder.FilterFunctionBuilder> functionList = new ArrayList<>();

        //gene category boost
        functionList.add(geneCategoryBoost());

        //human data boost
        functionList.add(humanSpeciesBoost());

        functionList.add(proteinCodingBoost());

        functionList.add(rnaBoost());

        functionList.add(pseudogeneBoost());

        functionList.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("name_key.keyword",q),
                ScoreFunctionBuilders.weightFactorFunction(1000F)));

        functionList.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("primaryKey",q),
                ScoreFunctionBuilders.weightFactorFunction(1000F)));

        functionList.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("name_key.keywordAutocomplete",q),
                ScoreFunctionBuilders.weightFactorFunction(500F)));

        functionList.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("name_key.standardBigrams",q),
                ScoreFunctionBuilders.weightFactorFunction(500F)));

        functionList.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("species",q),
                ScoreFunctionBuilders.weightFactorFunction(2F)));

        functionList.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("species.synonyms",q),
                ScoreFunctionBuilders.weightFactorFunction(2F)));

        functionList.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("automatedGeneSynopsis",q),
                ScoreFunctionBuilders.weightFactorFunction(1.5F)));

        functionList.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("diseases",q),
                ScoreFunctionBuilders.weightFactorFunction(1.2F)));

        functionList.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("diseasesWithParents",q),
                ScoreFunctionBuilders.weightFactorFunction(1.01F)));

        //per term boost, add a 'should' clause for each individual term
        List<String> tokens = tokenizeQuery(q);
        for (String token : tokens) {
            MultiMatchQueryBuilder mmq = multiMatchQuery(token);
            searchHelper.getSearchFields().stream().forEach(mmq::field);
            mmq.type(MultiMatchQueryBuilder.Type.CROSS_FIELDS);
            mmq.operator(Operator.AND);
            mmq.fields(searchHelper.getBoostMap());
            mmq.queryName(token);
            functionList.add(new FunctionScoreQueryBuilder.FilterFunctionBuilder(mmq, ScoreFunctionBuilders.weightFactorFunction(10.0F)));
        }


        return functionList.toArray(new FunctionScoreQueryBuilder.FilterFunctionBuilder[functionList.size()]);

    }

    private FunctionScoreQueryBuilder.FilterFunctionBuilder geneCategoryBoost() {
        return new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("category","gene"),
                ScoreFunctionBuilders.weightFactorFunction(1.1F));
    }

    private FunctionScoreQueryBuilder.FilterFunctionBuilder proteinCodingBoost() {
        return new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("soTermName","protein_coding_gene"),
                ScoreFunctionBuilders.weightFactorFunction(1.3F));
    }

    private FunctionScoreQueryBuilder.FilterFunctionBuilder rnaBoost() {
        return new FunctionScoreQueryBuilder.FilterFunctionBuilder(regexpQuery("soTermName",".*RNA.*_gene"),
                ScoreFunctionBuilders.weightFactorFunction(1.2F));
    }

    private FunctionScoreQueryBuilder.FilterFunctionBuilder pseudogeneBoost() {
        return new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("soTermName","pseudogene"),
                ScoreFunctionBuilders.weightFactorFunction(0.5F));
    }

    private FunctionScoreQueryBuilder.FilterFunctionBuilder humanSpeciesBoost() {
        return new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchQuery("species","Homo sapiens"),
                ScoreFunctionBuilders.weightFactorFunction(1.5F));
    }

    private FunctionScoreQueryBuilder.FilterFunctionBuilder documentHasDiseaseBoost() {
        return new FunctionScoreQueryBuilder.FilterFunctionBuilder(existsQuery("diseases"),
                ScoreFunctionBuilders.weightFactorFunction(1.1F));
    }

    private FunctionScoreQueryBuilder.FilterFunctionBuilder documentPopularityBoost() {
        FieldValueFactorFunctionBuilder popularity = ScoreFunctionBuilders.fieldValueFactorFunction("popularity");
        popularity.missing(1D);
        popularity.modifier(FieldValueFactorFunction.Modifier.SQRT);
        popularity.factor(1.1F);

        return new FunctionScoreQueryBuilder.FilterFunctionBuilder(matchAllQuery(), popularity);
    }

    public BoolQueryBuilder buildQuery(String queryTerm, String category, MultivaluedMap<String,String> filters) {

        BoolQueryBuilder bool = boolQuery();

        //handle the query input, if necessary
        if (StringUtils.isNotEmpty(queryTerm)) {

            queryTerm = queryManipulationService.processQuery(queryTerm);

            QueryStringQueryBuilder builder = queryStringQuery(queryTerm)
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
                entry.getValue().stream().forEach( value ->{
                            if(value.charAt(0) == '-'){
                                value = value.substring(1);
                                bool.mustNot(new TermQueryBuilder(entry.getKey() + ".keyword", value));
                            }else {
                                bool.filter(new TermQueryBuilder(entry.getKey() + ".keyword", value));
                            }
                    }
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

        //strip boolean tokens, empty strings and spaces strings
        List<String> tokensToRemove = new ArrayList<>();
        tokensToRemove.add("AND");
        tokensToRemove.add("OR");
        tokensToRemove.add("NOT");
        tokensToRemove.add("");

        tokens.removeAll(tokensToRemove);

        return tokens;


    }

    public void addRelatedDataLinks(List<Map<String,Object>> results) {
        results.stream().forEach(x -> addRelatedDataLinks(x));
    }

    public void addRelatedDataLinks(Map<String,Object> result) {
        String nameKey = (String) result.get("name_key");
        //String name = (String) result.get("name");
        String category = (String) result.get("category");

        List<RelatedDataLink> links = new ArrayList<>();

        if (StringUtils.equals(category,"gene")) {
            links.add(getRelatedDataLink("disease", "genes", nameKey));
            links.add(getRelatedDataLink("allele", "genes", nameKey));
            links.add(getRelatedDataLink("go", "go_genes", nameKey));
            links.add(getRelatedDataLink("model", "genes", nameKey));
        } else if (StringUtils.equals(category,"disease")) {
            links.add(getRelatedDataLink("gene", "diseasesWithParents", nameKey));
            links.add(getRelatedDataLink("allele", "diseasesWithParents", nameKey));
            links.add(getRelatedDataLink("model", "diseasesWithParents", nameKey));
        } else if (StringUtils.equals(category, "allele") && StringUtils.equals((String) result.get("alterationType"),"allele")) {
            links.add(getRelatedDataLink("disease", "alleles", nameKey));
            links.add(getRelatedDataLink("gene", "alleles", nameKey));
            links.add(getRelatedDataLink("model", "alleles", nameKey));
        } else if (StringUtils.equals(category,"model")) {
            links.add(getRelatedDataLink("gene","models", nameKey));
            links.add(getRelatedDataLink("allele","models", nameKey));
            links.add(getRelatedDataLink("disease","models", nameKey));
        } else if (StringUtils.equals(category,"go")) {
            String goType = (String) result.get("branch");
            if (StringUtils.equals(goType, "biological_process")) {
                links.add(getRelatedDataLink("gene", "biologicalProcessWithParents", nameKey,"Genes Annotated with this GO Term"));
            } else if (StringUtils.equals(goType, "molecular_function")) {
                links.add(getRelatedDataLink("gene", "molecularFunctionWithParents", nameKey,"Genes Annotated with this GO Term"));
            } else if (StringUtils.equals(goType, "cellular_component")) {
                links.add(getRelatedDataLink("gene", "cellularComponentWithParents", nameKey, "Genes Annotated with this GO Term"));
                links.add(getRelatedDataLink("gene", "cellularComponentExpressionWithParents", nameKey, "Genes Expressed in this Structure"));
            }
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

    private Boolean biotypeSelected(MultivaluedMap<String, String> filterMap) {
        if(filterMap.containsKey("biotypes")){
            List<String> biotypes = filterMap.get("biotypes");
            for(String value : biotypes){
                if(!searchHelper.isExcluded(value)){
                    return true;
                };
            }
        };
        return false;
    }
}
