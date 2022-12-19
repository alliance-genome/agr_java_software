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

	@Inject SearchService searchService;

	private Logger log = Logger.getLogger(getClass());

	@Override
	public SearchApiResponse search(String q, String category, Integer limit, Integer offset, String sort_by, Boolean debug, UriInfo uriInfo) {
		if (limit == null || limit == 0) { limit = 10; }
		if (offset == null ) { offset = 0; }
		if (q != null) { q = q.trim(); }
		if(debug == null) debug = false;
		if(debug) log.info("This is the Search query: " + q);
		else log.debug("This is the Search query: " + q);
		return searchService.query(q, category, limit, offset, sort_by, debug, uriInfo);
	}
	
}
