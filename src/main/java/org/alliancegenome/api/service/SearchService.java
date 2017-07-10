package org.alliancegenome.api.service;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.UriInfo;

import org.alliancegenome.api.dao.SearchDAO;
import org.alliancegenome.api.service.helper.SearchHelper;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;

@RequestScoped
public class SearchService {

	@Inject
	private SearchDAO searchDAO;

	@Inject
	private SearchHelper searchHelper;

	public String query(String q, String category, int limit, int offset, String sort_by, UriInfo uriInfo) {

		
		QueryBuilder query = searchHelper.buildQuery(q, category, uriInfo);

		HighlightBuilder hlb = searchHelper.buildHighlights();
		
		SearchResponse res = searchDAO.performQuery(query, limit, offset, hlb, sort_by);
		
		return res.toString();
	}
	
	
}
