package org.alliancegenome.api.controller;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.UriInfo;

import org.alliancegenome.api.rest.interfaces.SearchRESTInterface;
import org.alliancegenome.api.service.SearchService;
import org.alliancegenome.es.model.search.SearchResult;
import org.jboss.logging.Logger;

@RequestScoped
public class SearchController extends BaseController implements SearchRESTInterface {

    @Inject
    private SearchService searchService;

    private Logger log = Logger.getLogger(getClass());

    @Override
    public SearchResult search(String q, String category, int limit, int offset, String sort_by, UriInfo uriInfo) {
        if(limit == 0) limit = 10;
        log.info("This is the Search query: " + q);
        return searchService.query(q, category, limit, offset, sort_by, uriInfo);

    }

}
