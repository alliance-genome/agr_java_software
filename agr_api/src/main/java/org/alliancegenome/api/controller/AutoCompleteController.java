package org.alliancegenome.api.controller;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.rest.interfaces.AutoCompleteRESTInterface;
import org.alliancegenome.api.service.AutoCompleteService;
import org.alliancegenome.es.model.search.AutoCompleteResult;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestScoped
public class AutoCompleteController implements AutoCompleteRESTInterface {

	@Inject AutoCompleteService autoCompleteService;

	@Override
	public AutoCompleteResult searchAutoComplete(String q, String category) {
		log.info("This is the Auto Complete query: " + q);
		return autoCompleteService.query(q, category);
	}

}