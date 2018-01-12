package org.alliancegenome.api.dao.search;

import org.alliancegenome.api.dao.ESDAO;
import org.alliancegenome.api.model.query.Pagination;
import org.alliancegenome.api.model.query.SortBy;
import org.alliancegenome.api.model.search.SearchResult;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.script.Script;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.ScriptSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
import org.elasticsearch.search.sort.SortOrder;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@ApplicationScoped
public class DiseaseDAO extends ESDAO {

    private Logger log = Logger.getLogger(getClass());

    private static Map<SortBy, String> sortByMao = new LinkedHashMap<>();
    {
        sortByMao.put(SortBy.DISEASE, "diseaseName.keyword");
        sortByMao.put(SortBy.SPECIES, "disease_species.orderID");
        sortByMao.put(SortBy.GENE, "geneDocument.symbol.keyword");
    }

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
        searchRequestBuilder.setIndices(config.getEsIndex());

        // match on all disease terms who are the term or a child term
        // child terms have the parent Term ID in the field parentDiseaseIDs
        MatchQueryBuilder query = QueryBuilders.matchQuery("parentDiseaseIDs", diseaseID);

        // sort exact matches on the diseaseID at the top then all the child terms.
        SortBy sortBy = pagination.getSortBy();
        if (sortBy.equals(SortBy.DEFAULT)) {
            Script script = new Script("doc['diseaseID.keyword'].value == '" + diseaseID + "' ? 0 : 100");
            searchRequestBuilder.addSort(SortBuilders.scriptSort(script, ScriptSortBuilder.ScriptSortType.NUMBER));
            sortByMao.forEach((sortByColumn, columnName) -> {
                searchRequestBuilder.addSort(SortBuilders.fieldSort(columnName).order(getAscending(true)));
            });
        } else {
            // first ordering column
            searchRequestBuilder.addSort(SortBuilders.fieldSort(sortByMao.get(sortBy)).order(getAscending(pagination.getAsc())));
            Map<SortBy, String> newMap = sortByMao.entrySet().stream()
                    .filter(entry -> !entry.getKey().equals(sortBy))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
            newMap.forEach((sortColumn, s) ->
                    searchRequestBuilder.addSort(SortBuilders.fieldSort(sortByMao.get(sortColumn)).order(getAscending(pagination.getAsc())))
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
            hit.getSource().put("id", hit.getId());
            //hit.getSource().put("explain", hit.getExplanation());
            ret.add(hit.getSource());
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
            request.index(config.getEsIndex());
            GetResponse res = searchClient.get(request).get();
            //log.info(res);
            return res.getSource();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        return null;

    }

    public class SearchHitIterator implements Iterator<SearchHit> {

        private final SearchRequestBuilder initialRequest;

        private int searchHitCounter;
        private SearchHit[] currentPageResults;
        private int currentResultIndex;

        public SearchHitIterator(SearchRequestBuilder initialRequest) {
            this.initialRequest = initialRequest;
            this.searchHitCounter = 0;
            this.currentResultIndex = -1;
        }

        @Override
        public boolean hasNext() {
            if (currentPageResults == null || currentResultIndex + 1 >= currentPageResults.length) {
                SearchRequestBuilder paginatedRequestBuilder = initialRequest.setFrom(searchHitCounter);
                SearchResponse response = paginatedRequestBuilder.execute().actionGet();
                currentPageResults = response.getHits().getHits();

                if (currentPageResults.length < 1) return false;

                currentResultIndex = -1;
            }

            return true;
        }

        @Override
        public SearchHit next() {
            if (!hasNext()) return null;

            currentResultIndex++;
            searchHitCounter++;
            return currentPageResults[currentResultIndex];
        }

    }
}
