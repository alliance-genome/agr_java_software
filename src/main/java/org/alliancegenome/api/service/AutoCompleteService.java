package org.alliancegenome.api.service;

import org.alliancegenome.api.dao.search.AutoCompleteDAO;
import org.alliancegenome.api.model.search.AutoCompleteResult;
import org.alliancegenome.api.service.helper.AutoCompleteHelper;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.jboss.logging.Logger;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;


@RequestScoped
public class AutoCompleteService {

    private Logger log = Logger.getLogger(getClass());
    
    @Inject
    private AutoCompleteDAO autoCompleteDAO;
    
    @Inject
    private AutoCompleteHelper autoCompleteHelper;
    
    public AutoCompleteResult buildQuery(String q, String category) {

        QueryBuilder query = autoCompleteHelper.buildQuery(q, category);
        SearchResponse res = autoCompleteDAO.performQuery(query);
        AutoCompleteResult result = new AutoCompleteResult();
        result.results = autoCompleteHelper.formatResults(res);
        return result;
    }

}
