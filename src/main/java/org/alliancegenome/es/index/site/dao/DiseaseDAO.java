package org.alliancegenome.es.index.site.dao;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.index.ESDAO;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.es.model.query.SortBy;
import org.alliancegenome.es.model.search.SearchResult;
import org.alliancegenome.es.util.SearchHitIterator;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.ScriptSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class DiseaseDAO extends ESDAO {

    private Log log = LogFactory.getLog(getClass());

    public SearchResult getDiseaseAnnotations(String diseaseID, Pagination pagination) {

        SearchRequestBuilder searchRequestBuilder = getSearchRequestBuilder(diseaseID, pagination);
        searchRequestBuilder.setSize(pagination.getLimit());
        int fromIndex = pagination.getIndexOfFirstElement();
        searchRequestBuilder.setFrom(fromIndex);

        SearchResponse response = searchRequestBuilder.execute().actionGet();
        SearchResult result = new SearchResult();

        result.total = response.getHits().totalHits;
        result.results = formatResults(response);
        return result;
    }

    private SearchRequestBuilder getSearchRequestBuilder(String diseaseID, Pagination pagination) {
        SearchRequestBuilder searchRequestBuilder = searchClient.prepareSearch();

        //searchRequestBuilder.setExplain(true);
        searchRequestBuilder.setIndices(ConfigHelper.getEsIndex());

        // match on all disease terms who are the term or a child term
        // child terms have the parent Term ID in the field parentDiseaseIDs
        TermQueryBuilder builder = QueryBuilders.termQuery("parentDiseaseIDs", diseaseID);
        BoolQueryBuilder query = QueryBuilders.boolQuery().must(builder);

        BoolQueryBuilder fieldFilterQuery = QueryBuilders.boolQuery();
        diseaseFieldFilterMap.forEach((filter, fieldNames) -> {
                    String rawValue = pagination.getFieldFilterValueMap().get(filter);
                    if (rawValue != null) {
                        // match multiple fields with prefix type
                        String[] fieldNameArray = fieldNames.toArray(new String[fieldNames.size()]);
                        MultiMatchQueryBuilder multiBuilder = QueryBuilders.multiMatchQuery(rawValue.toLowerCase(), fieldNameArray)
                                .type(MultiMatchQueryBuilder.Type.PHRASE_PREFIX);
                        fieldFilterQuery.must(multiBuilder);
                    }
                }
        );
        query.must(fieldFilterQuery);

        // sort exact matches on the diseaseID at the top then all the child terms.
        String sortBy = pagination.getSortBy();
        if (StringUtils.isEmpty(sortBy)) {
            Script script = new Script("doc['diseaseID.keyword'].value == '" + diseaseID + "' ? 0 : 100");
            searchRequestBuilder.addSort(SortBuilders.scriptSort(script, ScriptSortBuilder.ScriptSortType.NUMBER));
            searchRequestBuilder.addSort(SortBuilders.fieldSort(diseaseFieldFilterSortingMap.get(FieldFilter.SPECIES)).order(getAscending(true)));
        } else {
            diseaseFieldFilterSortingMap.entrySet().stream()
                    .filter(entry -> entry.getKey().getName().equals(sortBy))
                    .forEach(entrySet ->
                            searchRequestBuilder.addSort(SortBuilders.fieldSort(entrySet.getValue()).order(getAscending(pagination.getAsc())))
                    );
        }

        searchRequestBuilder.setQuery(query);
        log.debug(searchRequestBuilder);
        return searchRequestBuilder;
    }

    private SortOrder getAscending(Boolean ascending) {
        return ascending ? SortOrder.ASC : SortOrder.DESC;
    }

    private ArrayList<Map<String, Object>> formatResults(SearchResponse response) {

        log.debug("Formatting Results: ");
        ArrayList<Map<String, Object>> ret = new ArrayList<>();

        for (SearchHit hit : response.getHits()) {
            hit.getSourceAsMap().put("id", hit.getId());
            //hit.getSource().put("explain", hit.getExplanation());
            ret.add(hit.getSourceAsMap());
        }
        log.debug("Finished Formatting Results: ");
        return ret;
    }


    public SearchHitIterator getDiseaseAnnotationsDownload(String id, Pagination pagination) {
        SearchRequestBuilder searchRequestBuilder = getSearchRequestBuilder(id, pagination);
        SearchHitIterator hitIterator = new SearchHitIterator(searchRequestBuilder);
        return hitIterator;
    }

    // This class is going to get replaced by a call to NEO

    public Map<String, Object> getById(String id) {

        try {
            GetRequest request = new GetRequest();
            request.id(id);
            request.type("disease");
            request.index(ConfigHelper.getEsIndex());
            GetResponse res = searchClient.get(request).get();
            //log.info(res);
            return res.getSource();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return null;

    }

    private static Map<FieldFilter, String> diseaseFieldFilterSortingMap = new HashMap<>(10);

    static {
        diseaseFieldFilterSortingMap.put(FieldFilter.GENE_NAME, "geneDocument.symbol.sort");
        diseaseFieldFilterSortingMap.put(FieldFilter.DISEASE, "diseaseName.keyword");
        diseaseFieldFilterSortingMap.put(FieldFilter.SPECIES, "geneDocument.species.sort");
    }

    private static Map<FieldFilter, List<String>> diseaseFieldFilterMap = new HashMap<>(10);

    static {
        diseaseFieldFilterMap.put(FieldFilter.GENE_NAME, Collections.singletonList("geneDocument.symbol"));
        diseaseFieldFilterMap.put(FieldFilter.DISEASE, Collections.singletonList("diseaseName"));
        diseaseFieldFilterMap.put(FieldFilter.SPECIES, Collections.singletonList("geneDocument.species"));
        diseaseFieldFilterMap.put(FieldFilter.ASSOCIATION_TYPE, Collections.singletonList("associationType"));
        diseaseFieldFilterMap.put(FieldFilter.GENETIC_ENTITY, Collections.singletonList("featureDocument.symbol"));
        diseaseFieldFilterMap.put(FieldFilter.GENETIC_ENTITY_TYPE, Collections.singletonList("featureDocument.category.autocomplete"));
        diseaseFieldFilterMap.put(FieldFilter.REFERENCE, Arrays.asList("publications.pubModId", "publications.pubMedId"));
        diseaseFieldFilterMap.put(FieldFilter.EVIDENCE_CODE, Collections.singletonList("publications.evidenceCodes"));
        diseaseFieldFilterMap.put(FieldFilter.SOURCE, Collections.singletonList("source.name"));
    }

}
