package org.alliancegenome.api.controller;

import javax.enterprise.context.RequestScoped;
import javax.json.JsonObject;

import org.alliancegenome.api.rest.SearchAutoCompleteRESTInterface;

@RequestScoped
public class SearchAutoCompleteController implements SearchAutoCompleteRESTInterface {

	@Override
	public JsonObject searchAutoComplete(String q, String category) {
		System.out.println("This is the query: " + q);
		System.out.println("This is the category: " + category);
		return null;
	}

}