package org.alliancegenome.api.service;

import javax.enterprise.context.RequestScoped;

import org.alliancegenome.api.service.helper.AutoCompleteHelper;
import org.alliancegenome.es.index.site.dao.AutoCompleteDAO;
import org.alliancegenome.es.model.search.AutoCompleteResult;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;

@RequestScoped
public class AutoCompleteService {

    private static AutoCompleteDAO autoCompleteDAO = new AutoCompleteDAO();
    private static AutoCompleteHelper autoCompleteHelper = new AutoCompleteHelper();

    public AutoCompleteResult buildQuery(String q, String category) {

        QueryBuilder query = autoCompleteHelper.buildQuery(q, category);
        SearchResponse res = autoCompleteDAO.performQuery(query);
        AutoCompleteResult result = new AutoCompleteResult();
        result.results = autoCompleteHelper.formatResults(res);
        return result;
    }

}
