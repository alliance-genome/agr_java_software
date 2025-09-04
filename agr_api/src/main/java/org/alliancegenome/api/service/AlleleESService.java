package org.alliancegenome.api.service;

import jakarta.enterprise.context.RequestScoped;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.curation_api.model.document.es.TransgenicAlleleDocument;
import org.alliancegenome.es.model.query.Pagination;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;


@RequestScoped
public class AlleleESService extends ESService {

	public JsonResultResponse<TransgenicAlleleDocument> getTransgenicAlleles(String alleleId) {
		BoolQueryBuilder bool = boolQuery();
		BoolQueryBuilder bool2 = boolQuery();
		bool.must(bool2);
		// ToDo: Change this class such that the category is public
		// TransgenicAlleleDocument.category
		bool.filter(new TermQueryBuilder("category", "transgenic_allele_annotation"));
		bool2.should(new MatchQueryBuilder("allele.primaryExternalId.keyword", alleleId));

		JsonResultResponse<TransgenicAlleleDocument> ret = new JsonResultResponse<>();

		SearchResponse searchResponse = getSearchResponse(bool, new Pagination(), null, false);
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);
		List<TransgenicAlleleDocument> list = new ArrayList<>();
		Arrays.stream(searchResponse.getHits().getHits())
			.forEach(searchHit -> {
				try {
					TransgenicAlleleDocument object = mapper.readValue(searchHit.getSourceAsString(), TransgenicAlleleDocument.class);
					list.add(object);
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
		ret.setResults(list);
		return ret;
	}


}
