package org.alliancegenome.api.controller;

import org.alliancegenome.api.rest.interfaces.SearchRESTInterface;
import org.alliancegenome.api.service.SearchService;
import org.alliancegenome.es.model.search.SearchApiResponse;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.UriInfo;

@RequestScoped
public class SearchController implements SearchRESTInterface {

	@Inject SearchService searchService;

	@Override
	public SearchApiResponse search(String q, String category, Integer limit, Integer offset, String sort_by, Boolean debug, UriInfo uriInfo) {
		if (limit == null || limit == 0) { limit = 10; }
		if (offset == null ) { offset = 0; }
		if (q != null) { q = q.trim(); }
		if(debug == null) debug = false;
		if(debug) Log.info("This is the Search query: " + q);
		else Log.debug("This is the Search query: " + q);
		return searchService.query(q, category, limit, offset, sort_by, debug, uriInfo);
	}
	
}
