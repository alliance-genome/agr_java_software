package org.alliancegenome.api.service;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.dao.AutoCompleteDAO;
import org.alliancegenome.api.service.helper.AutoCompleteHelper;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;

@RequestScoped
public class AutoCompleteService {

	@Inject
	private AutoCompleteDAO autoCompleteDAO;
	
	@Inject
	private AutoCompleteHelper autoCompleteHelper;
	
	public String buildQuery(String q, String category) {
		System.out.println("This is the query: " + q);
		System.out.println("This is the category: " + category);
		
		QueryBuilder query = autoCompleteHelper.buildQuery(q, category);


		SearchResponse res = autoCompleteDAO.performQuery("searchable_items_blue", query);
		
		return res.toString();
		

	}

}
