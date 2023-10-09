package org.alliancegenome.api.controller;

import org.alliancegenome.api.rest.interfaces.AutoCompleteRESTInterface;
import org.alliancegenome.api.service.AutoCompleteService;
import org.alliancegenome.es.model.search.AutoCompleteResult;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
 
@RequestScoped
public class AutoCompleteController implements AutoCompleteRESTInterface {

	@Inject AutoCompleteService autoCompleteService;

	@Override
	public AutoCompleteResult searchAutoComplete(String q, String category) {
		Log.info("This is the Auto Complete query: " + q);
		return autoCompleteService.query(q, category);
	}

}