package org.alliancegenome.api.dao;

import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.jboss.logging.Logger;

import javax.enterprise.context.ApplicationScoped;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
@SuppressWarnings("serial")
public class AutoCompleteDAO extends ESDAO {

    private Logger log = Logger.getLogger(getClass());
    
    private List<String> response_fields = new ArrayList<String>() {
        {
            add("name_key"); add("name"); add("symbol");
            add("primaryId"); add("category"); add("go_type");
        }
    };
    
    public SearchResponse performQuery(QueryBuilder query) {
        SearchRequestBuilder srb = searchClient.prepareSearch();
        srb.setFetchSource(response_fields.toArray(new String[response_fields.size()]), null);
        srb.setIndices(config.getEsIndex());
        srb.setQuery(query);
        SearchResponse res = srb.execute().actionGet();
        return res;
    }
    
}
