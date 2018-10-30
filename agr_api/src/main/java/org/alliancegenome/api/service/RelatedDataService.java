package org.alliancegenome.api.service;

import org.alliancegenome.es.index.site.dao.SearchDAO;

import javax.enterprise.context.RequestScoped;
import java.util.List;
import java.util.Map;

@RequestScoped
public class RelatedDataService {

    private SearchDAO searchDAO = new SearchDAO();

    public void addRelatedDataLinks(List<Map<String,Object>> results) {
        results.stream().forEach(x -> addRelatedDataLinks(x));
    }

    public void addRelatedDataLinks(Map<String,Object> result) {

    }

}