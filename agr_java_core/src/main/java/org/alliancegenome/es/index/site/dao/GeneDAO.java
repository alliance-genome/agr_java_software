package org.alliancegenome.es.index.site.dao;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.index.ESDAO;
import org.alliancegenome.es.index.site.document.PhenotypeAnnotationDocument;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.es.model.search.SearchApiResponse;
import org.alliancegenome.es.util.SearchHitIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.*;
import org.elasticsearch.search.sort.SortBuilders;

import java.util.*;
import java.util.concurrent.ExecutionException;

public class GeneDAO extends ESDAO {

    private Log log = LogFactory.getLog(getClass());

    // This class is going to get replaced by a call to NEO

    public Map<String, Object> getById(String id) {
        try {
            GetRequest request = new GetRequest();
            request.id(id);
            request.type("gene");
            request.index(ConfigHelper.getEsIndex());
            GetResponse res = searchClient.get(request).get();
            //log.info(res);
            return res.getSource();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    public Map<String, Object> getGeneBySecondary(String id) {
        SearchRequestBuilder searchRequestBuilder = searchClient.prepareSearch();

        //searchRequestBuilder.setExplain(true);
        searchRequestBuilder.setIndices(ConfigHelper.getEsIndex());

        // match on secondary IDs
        MatchQueryBuilder query = QueryBuilders.matchQuery("secondaryIds", id);
        searchRequestBuilder.setQuery(query);
        org.elasticsearch.action.search.SearchResponse response = searchRequestBuilder.execute().actionGet();
        long total = response.getHits().totalHits;
        if (total > 0)
            return formatResults(response).get(0);
        else
            return null;
    }

    public SearchApiResponse getAllelesByGene(String geneId, Pagination pagination) {
        SearchRequestBuilder searchRequestBuilder = getSearchRequestBuilder(geneId);
        if (pagination != null) {
            searchRequestBuilder.setSize(pagination.getLimit());
            int fromIndex = pagination.getIndexOfFirstElement();
            searchRequestBuilder.setFrom(fromIndex);
        }
        org.elasticsearch.action.search.SearchResponse response = searchRequestBuilder.execute().actionGet();
        SearchApiResponse result = new SearchApiResponse();

        result.total = response.getHits().totalHits;
        result.resultMapList = formatResults(response);
        return result;
    }

    private SearchRequestBuilder getSearchRequestBuilder(String geneID) {
        SearchRequestBuilder searchRequestBuilder = searchClient.prepareSearch();

        //searchRequestBuilder.setExplain(true);
        searchRequestBuilder.setIndices(ConfigHelper.getEsIndex());

        TermQueryBuilder builder = QueryBuilders.termQuery("geneDocument.primaryId", geneID);
        BoolQueryBuilder query = QueryBuilders.boolQuery().must(builder);
        MultiMatchQueryBuilder multiBuilder = QueryBuilders.multiMatchQuery("allele", "category")
                .type(MultiMatchQueryBuilder.Type.PHRASE_PREFIX);
        query.must(multiBuilder);
        searchRequestBuilder.addSort(SortBuilders.fieldSort("symbol.sort"));
        searchRequestBuilder.setQuery(query);
        log.debug(searchRequestBuilder);
        return searchRequestBuilder;
    }

    public SearchApiResponse getPhenotypeAnnotations(String geneID, Pagination pagination) {
        SearchRequestBuilder searchRequestBuilder = getSearchRequestBuilderPhenotype(geneID, pagination);
        return getSearchResult(pagination, searchRequestBuilder);
    }

    private SearchRequestBuilder getSearchRequestBuilderPhenotype(String geneID, Pagination pagination) {

        SearchRequestBuilder searchRequestBuilder = searchClient.prepareSearch();
        //searchRequestBuilder.setExplain(true);
        searchRequestBuilder.setIndices(ConfigHelper.getEsIndex());

        // match on termName
        // child terms have the parent Term ID in the field parentDiseaseIDs
        TermQueryBuilder builder = QueryBuilders.termQuery("category", PhenotypeAnnotationDocument.CATEGORY);
        BoolQueryBuilder query = QueryBuilders.boolQuery().must(builder);
        query.must(QueryBuilders.termQuery("geneDocument.primaryId", geneID));

        phenotypeFieldFilterMap.forEach((filter, fieldNames) -> {
                    String rawValue = pagination.getFieldFilterValueMap().get(filter);
                    if (rawValue != null) {
                        // match multiple fields with prefix type
                        String[] fieldNameArray = fieldNames.toArray(new String[fieldNames.size()]);
                        AbstractQueryBuilder termBuilder;
                        if (fieldNameArray.length > 1) {
                            termBuilder = QueryBuilders.multiMatchQuery(rawValue.toLowerCase(), fieldNameArray)
                                    .type(MultiMatchQueryBuilder.Type.PHRASE_PREFIX);
                        } else {
                            termBuilder = QueryBuilders.prefixQuery(fieldNameArray[0], rawValue.toLowerCase());
                        }
                        query.must(termBuilder);
                    }
                }
        );

        diseaseFieldFilterSortingMap.entrySet().stream()
                .filter(entry -> entry.getKey().getName().equals(pagination.getSortBy()))
                .forEach(entrySet ->
                        searchRequestBuilder.addSort(SortBuilders.fieldSort(entrySet.getValue()).order(getAscending(pagination.getAsc())))
                );

        searchRequestBuilder.setQuery(query);
        log.debug(searchRequestBuilder);
        return searchRequestBuilder;
    }


    private static Map<FieldFilter, String> diseaseFieldFilterSortingMap = new HashMap<>(10);

    static {
        diseaseFieldFilterSortingMap.put(FieldFilter.PHENOTYPE, "phenotype.sort");
        diseaseFieldFilterSortingMap.put(FieldFilter.GENETIC_ENTITY, "featureDocument.symbol.sort");
    }

    private static Map<FieldFilter, List<String>> phenotypeFieldFilterMap = new HashMap<>(10);

    static {
        phenotypeFieldFilterMap.put(FieldFilter.PHENOTYPE, Collections.singletonList("phenotype.standardText"));
        phenotypeFieldFilterMap.put(FieldFilter.GENETIC_ENTITY, Collections.singletonList("featureDocument.searchSymbol"));
        phenotypeFieldFilterMap.put(FieldFilter.GENETIC_ENTITY_TYPE, Arrays.asList("featureDocument.category.autocomplete", "geneDocument.category.autocomplete"));
        phenotypeFieldFilterMap.put(FieldFilter.REFERENCE, Arrays.asList("publications.pubModId.standardText", "publications.pubMedId.standardText"));
    }

    public SearchHitIterator getPhenotypeAnnotationsDownload(String id, Pagination pagination) {
        SearchRequestBuilder searchRequestBuilder = getSearchRequestBuilderPhenotype(id, pagination);
        return new SearchHitIterator(searchRequestBuilder);
    }


}