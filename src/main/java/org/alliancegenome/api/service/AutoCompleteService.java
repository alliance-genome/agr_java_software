package org.alliancegenome.api.service;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.service.helper.AutoCompleteHelper;
import org.alliancegenome.shared.es.dao.site_index.AutoCompleteDAO;
import org.alliancegenome.shared.es.model.search.AutoCompleteResult;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;


@RequestScoped
public class AutoCompleteService {

    //private Logger log = Logger.getLogger(getClass());

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
