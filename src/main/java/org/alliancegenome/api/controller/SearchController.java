package org.alliancegenome.api.controller;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.UriInfo;

import org.alliancegenome.api.rest.interfaces.SearchRESTInterface;
import org.alliancegenome.api.service.SearchService;

@RequestScoped
public class SearchController implements SearchRESTInterface {
	
	@Inject
	private SearchService searchService;
	
	@Override
	public String search(String q, String category, int limit, int offset, String sort_by, UriInfo uriInfo) {
		if(limit == 0) limit = 10;
		return searchService.query(q, category, limit, offset, sort_by, uriInfo);
		
	}
	
}
