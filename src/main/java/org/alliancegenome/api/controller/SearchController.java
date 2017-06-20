package org.alliancegenome.api.controller;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.rest.SearchRESTInterface;
import org.alliancegenome.api.service.SearchService;

@RequestScoped
public class SearchController implements SearchRESTInterface {
	
	@Inject
	private SearchService searchService;
	
	
}
