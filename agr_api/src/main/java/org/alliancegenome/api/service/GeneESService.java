package org.alliancegenome.api.service;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

import org.alliancegenome.curation_api.model.document.es.GeneSummaryDocument;
import org.alliancegenome.es.model.query.Pagination;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class GeneESService extends ESService {

	public GeneSummaryDocument getById(String geneId) {

		BoolQueryBuilder bool = boolQuery();
		bool.must(new MatchQueryBuilder("gene.primaryExternalId", geneId));
		bool.filter(new TermQueryBuilder("category", "gene_summary"));
		Pagination pagination = new Pagination();
		SearchResponse searchResponse = getSearchResponse(bool, pagination, null, false);
		try {
			if (searchResponse.getHits().getTotalHits().value >= 1) {
				return mapper.readValue(searchResponse.getHits().getHits()[0].getSourceAsString(), GeneSummaryDocument.class);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;

	}
}
