package org.alliancegenome.api.controller;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.json.JsonObject;

import org.alliancegenome.api.rest.interfaces.SearchAutoCompleteRESTInterface;
import org.alliancegenome.api.service.AutoCompleteService;

@RequestScoped
public class SearchAutoCompleteController implements SearchAutoCompleteRESTInterface {

	@Inject
	private AutoCompleteService autoCompleteService;
	
	@Override
	public String searchAutoComplete(String q, String category) {
		return autoCompleteService.buildQuery(q, category);
	}

}