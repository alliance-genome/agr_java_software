package org.alliancegenome.es.index.site.dao;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.index.ESDAO;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.es.model.search.SearchApiResponse;
import org.alliancegenome.es.util.SearchHitIterator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.index.query.AbstractQueryBuilder;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MultiMatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.sort.ScriptSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;

public class DiseaseDAO extends ESDAO {

    private Log log = LogFactory.getLog(getClass());

    public SearchApiResponse getDiseaseAnnotations(String diseaseID, Pagination pagination) {

        SearchRequestBuilder searchRequestBuilder = getSearchRequestBuilder(diseaseID, pagination);
        return getSearchResult(pagination, searchRequestBuilder);

    }

    private SearchRequestBuilder getSearchRequestBuilder(String diseaseID, Pagination pagination) {
        SearchRequestBuilder searchRequestBuilder = searchClient.prepareSearch();

        //searchRequestBuilder.setExplain(true);
        searchRequestBuilder.setIndices(ConfigHelper.getEsIndex());

        // match on all disease terms who are the term or a child term
        // child terms have the parent Term ID in the field parentDiseaseIDs
        TermQueryBuilder builder = QueryBuilders.termQuery("parentDiseaseIDs", diseaseID);
        BoolQueryBuilder query = QueryBuilders.boolQuery().must(builder);

        diseaseFieldFilterMap.forEach((filter, fieldNames) -> {
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

        // sort exact matches on the diseaseID at the top then all the child terms.
        if (pagination.sortByDefault()) {
            Script script = new Script("doc['diseaseID.keyword'].value == '" + diseaseID + "' ? 0 : 100");
            searchRequestBuilder.addSort(SortBuilders.scriptSort(script, ScriptSortBuilder.ScriptSortType.NUMBER));
            searchRequestBuilder.addSort(SortBuilders.fieldSort(diseaseFieldFilterSortingMap.get(FieldFilter.SPECIES_DEFAULT)).order(getAscending(true)));
            searchRequestBuilder.addSort(SortBuilders.fieldSort(diseaseFieldFilterSortingMap.get(FieldFilter.GENE_NAME)).order(getAscending(true)));
        } else {
            diseaseFieldFilterSortingMap.entrySet().stream()
                    .filter(entry -> entry.getKey().getName().equals(pagination.getSortBy()))
                    .forEach(entrySet ->
                            searchRequestBuilder.addSort(SortBuilders.fieldSort(entrySet.getValue()).order(getAscending(pagination.getAsc())))
                    );
        }

        searchRequestBuilder.setQuery(query);
        log.debug(searchRequestBuilder);
        return searchRequestBuilder;
    }

    public SearchHitIterator getDiseaseAnnotationsDownload(String id, Pagination pagination) {
        SearchRequestBuilder searchRequestBuilder = getSearchRequestBuilder(id, pagination);
        return new SearchHitIterator(searchRequestBuilder);
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
        diseaseFieldFilterSortingMap.put(FieldFilter.SPECIES_DEFAULT, "source.species.orderID");
        diseaseFieldFilterSortingMap.put(FieldFilter.SPECIES, "source.species.name.sort");
    }

    private static Map<FieldFilter, List<String>> diseaseFieldFilterMap = new HashMap<>(10);

    static {
        diseaseFieldFilterMap.put(FieldFilter.GENE_NAME, Collections.singletonList("geneDocument.symbol"));
        diseaseFieldFilterMap.put(FieldFilter.DISEASE, Collections.singletonList("diseaseName"));
        diseaseFieldFilterMap.put(FieldFilter.SPECIES, Collections.singletonList("geneDocument.species"));
        diseaseFieldFilterMap.put(FieldFilter.ASSOCIATION_TYPE, Collections.singletonList("associationType.standardText"));
        diseaseFieldFilterMap.put(FieldFilter.GENETIC_ENTITY, Collections.singletonList("alleleDocument.symbol"));
        diseaseFieldFilterMap.put(FieldFilter.GENETIC_ENTITY_TYPE, Collections.singletonList("alleleDocument.category.autocomplete"));
        diseaseFieldFilterMap.put(FieldFilter.REFERENCE, Arrays.asList("publications.pubModId.standardText", "publications.pubMedId.standardText"));
        diseaseFieldFilterMap.put(FieldFilter.EVIDENCE_CODE, Collections.singletonList("publications.evidenceCodes"));
        diseaseFieldFilterMap.put(FieldFilter.SOURCE, Collections.singletonList("source.name"));
    }

}
