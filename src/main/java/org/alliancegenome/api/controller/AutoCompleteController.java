package org.alliancegenome.api.controller;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.rest.interfaces.AutoCompleteRESTInterface;
import org.alliancegenome.api.service.AutoCompleteService;

@RequestScoped
public class AutoCompleteController implements AutoCompleteRESTInterface {

	@Inject
	private AutoCompleteService autoCompleteService;
	
	@Override
	public String searchAutoComplete(String q, String category) {
		return autoCompleteService.buildQuery(q, category);
	}

}