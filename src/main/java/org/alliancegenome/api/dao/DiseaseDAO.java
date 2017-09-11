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
import java.util.Map;

@ApplicationScoped
public class DiseaseDAO extends ESDAO {

    private Logger log = Logger.getLogger(getClass());

    public SearchResult getDiseaseAnnotations(String diseaseID, int offset, int limit) {

        SearchRequestBuilder searchRequestBuilder = searchClient.prepareSearch();

        searchRequestBuilder.setExplain(true);
        searchRequestBuilder.setIndices(config.getEsIndex());
        searchRequestBuilder.setSize(limit);
        searchRequestBuilder.setFrom(offset);

        // match on all disease terms who are the term or a child term
        // child terms have the parent Term ID in the field parentDiseaseIDs
        MatchQueryBuilder query = QueryBuilders.matchQuery("parentDiseaseIDs", diseaseID);

        // sort exact matches on the diseaseID at the top then all the child terms.
        Script script = new Script("doc['diseaseID.raw'].value == '" + diseaseID + "' ? 0 : 100");
        searchRequestBuilder.addSort(SortBuilders.scriptSort(script, ScriptSortBuilder.ScriptSortType.NUMBER));

        searchRequestBuilder.addSort(SortBuilders.fieldSort("disease_species.orderID").order(SortOrder.ASC));
        searchRequestBuilder.addSort(SortBuilders.fieldSort("geneDocument.symbol.raw").order(SortOrder.ASC));
        searchRequestBuilder.setQuery(query);

        log.debug(searchRequestBuilder);

        SearchResponse response = searchRequestBuilder.execute().actionGet();
        SearchResult result = new SearchResult();

        result.total = response.getHits().totalHits;
        result.results = formatResults(response);
        return result;
    }

    private ArrayList<Map<String, Object>> formatResults(SearchResponse response) {

        log.info("Formatting Results: ");
        ArrayList<Map<String, Object>> ret = new ArrayList<>();

        for (SearchHit hit : response.getHits()) {
            hit.getSource().put("id", hit.getId());
            //hit.getSource().put("explain", hit.getExplanation());
            ret.add(hit.getSource());
        }
        log.info("Finished Formatting Results: ");
        return ret;
    }


}
