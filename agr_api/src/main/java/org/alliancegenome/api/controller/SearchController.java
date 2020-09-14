package org.alliancegenome.api.controller;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.UriInfo;

import org.alliancegenome.api.rest.interfaces.SearchRESTInterface;
import org.alliancegenome.api.service.SearchService;
import org.alliancegenome.es.model.search.SearchApiResponse;
import org.jboss.logging.Logger;

@RequestScoped
public class SearchController implements SearchRESTInterface {

    @Inject
    private SearchService searchService;

    private Logger log = Logger.getLogger(getClass());

    @Override
    public SearchApiResponse search(String q, String category, Integer limit, Integer offset, String sort_by, UriInfo uriInfo) {
        if (limit == null || limit == 0) { limit = 10; }
        if (q != null) { q = q.trim(); }
        log.info("This is the Search query: " + q);
        return searchService.query(q, category, limit, offset, sort_by, uriInfo);
    }

}
