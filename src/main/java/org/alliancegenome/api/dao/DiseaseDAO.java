package org.alliancegenome.api.dao;

import org.alliancegenome.api.model.SearchResult;
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
import java.util.Map;

@ApplicationScoped
public class DiseaseDAO extends ESDAO {

    private Logger log = Logger.getLogger(getClass());

    public SearchResult getDiseaseAnnotations(String diseaseID, int offset, int limit) {

        SearchRequestBuilder searchRequestBuilder = getSearchRequestBuilder(diseaseID);
        searchRequestBuilder.setSize(limit);
        searchRequestBuilder.setFrom(offset);

        SearchResponse response = searchRequestBuilder.execute().actionGet();
        SearchResult result = new SearchResult();

        result.total = response.getHits().totalHits;
        result.results = formatResults(response);
        return result;
    }

    private SearchRequestBuilder getSearchRequestBuilder(String diseaseID) {
        SearchRequestBuilder searchRequestBuilder = searchClient.prepareSearch();

        searchRequestBuilder.setExplain(true);
        searchRequestBuilder.setIndices(config.getEsIndex());

        // match on all disease terms who are the term or a child term
        // child terms have the parent Term ID in the field parentDiseaseIDs
        MatchQueryBuilder query = QueryBuilders.matchQuery("parentDiseaseIDs", diseaseID);

        // sort exact matches on the diseaseID at the top then all the child terms.
        Script script = new Script("doc['diseaseID.keyword'].value == '" + diseaseID + "' ? 0 : 100");
        searchRequestBuilder.addSort(SortBuilders.scriptSort(script, ScriptSortBuilder.ScriptSortType.NUMBER));

        searchRequestBuilder.addSort(SortBuilders.fieldSort("diseaseName.keyword").order(SortOrder.ASC));
        searchRequestBuilder.addSort(SortBuilders.fieldSort("disease_species.orderID").order(SortOrder.ASC));
        searchRequestBuilder.addSort(SortBuilders.fieldSort("geneDocument.symbol.keyword").order(SortOrder.ASC));
        searchRequestBuilder.setQuery(query);

        log.debug(searchRequestBuilder);
        return searchRequestBuilder;
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


    public SearchHitIterator getDiseaseAnnotationsDownload(String id) {
        SearchRequestBuilder searchRequestBuilder = getSearchRequestBuilder(id);
        SearchHitIterator hitIterator = new SearchHitIterator(searchRequestBuilder);
        return hitIterator;
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
