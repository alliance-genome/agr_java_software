package org.alliancegenome.api.service;

import org.alliancegenome.es.index.site.dao.SearchDAO;
import org.alliancegenome.es.model.search.RelatedDataLink;

import javax.enterprise.context.RequestScoped;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import java.util.List;
import java.util.Map;

@RequestScoped
public class RelatedDataService {

    private SearchDAO searchDAO = new SearchDAO();
    private SearchService searchService = new SearchService();

    public void addRelatedDataLinks(List<Map<String,Object>> results) {
        results.stream().forEach(x -> addRelatedDataLinks(x));
    }

    public void addRelatedDataLinks(Map<String,Object> result) {

    }

    public RelatedDataLink getRelatedDataLink(String category, String targetField, String sourceName) {
        MultivaluedMap<String,String> filters = new MultivaluedHashMap<>();

        filters.add(targetField, sourceName);

        Long count = searchDAO.performCountQuery(searchService.buildQuery(null, category, filters));

        RelatedDataLink relatedDataLink = new RelatedDataLink();
        relatedDataLink.setCategory(category);
        relatedDataLink.setTargetField(targetField);
        relatedDataLink.setSourceName(sourceName);
        relatedDataLink.setCount(count);

        return relatedDataLink;
    }

}