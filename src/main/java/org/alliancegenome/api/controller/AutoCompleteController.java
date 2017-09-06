package org.alliancegenome.api.controller;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.model.AutoCompleteResult;
import org.alliancegenome.api.rest.interfaces.AutoCompleteRESTInterface;
import org.alliancegenome.api.service.AutoCompleteService;
import org.jboss.logging.Logger;

@RequestScoped
public class AutoCompleteController implements AutoCompleteRESTInterface {

	@Inject
	private AutoCompleteService autoCompleteService;
	
	private Logger log = Logger.getLogger(getClass());
	
	@Override
	public AutoCompleteResult searchAutoComplete(String q, String category) {
		log.info("This is the Auto Complete query: " + q);
		return autoCompleteService.buildQuery(q, category);
	}

}