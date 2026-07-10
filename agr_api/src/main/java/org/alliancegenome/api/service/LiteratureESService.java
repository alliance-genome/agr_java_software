package org.alliancegenome.api.service;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

import org.alliancegenome.core.document.LiteratureSummaryDocument;
import org.alliancegenome.api.es.query.Pagination;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class LiteratureESService extends ESService {

	public LiteratureSummaryDocument getById(String id) {
		BoolQueryBuilder bool = boolQuery();
		bool.must(new MatchQueryBuilder("literatureSummary.curie", id));
		bool.filter(new TermQueryBuilder("category", "literature_summary"));
		Pagination pagination = new Pagination();
		SearchResponse searchResponse = getSearchResponse(bool, pagination, null, false);
		try {
			if (searchResponse.getHits().getTotalHits().value >= 1) {
				return mapper.readValue(searchResponse.getHits().getHits()[0].getSourceAsString(), LiteratureSummaryDocument.class);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

}
