package org.alliancegenome.api.service;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;

import org.alliancegenome.api.entity.GeneToGeneParalogyDocument;
import org.alliancegenome.api.response.JsonResultResponse;
import org.alliancegenome.api.es.dao.SearchDAO;
import org.alliancegenome.api.es.query.Pagination;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class GeneToGeneParalogyESService {
	
	@Inject ObjectMapper mapper;
	private static final SearchDAO searchDAO = new SearchDAO();

	private SearchResponse getSearchResponse(BoolQueryBuilder bool, Pagination pagination, LinkedHashMap<String, SortOrder> sorts, boolean debug) {
		List<AggregationBuilder> aggBuilders = new ArrayList<>();
		HighlightBuilder hlb = new HighlightBuilder();

		return searchDAO.performQuery(
			bool, aggBuilders, null, List.of("*"),
			pagination.getLimit(), pagination.getOffset(), hlb, sorts, false);

	}

	public JsonResultResponse<GeneToGeneParalogyDocument> getParalogyMultiGeneJson(List<String> geneIds, Pagination pagination) {
		long start = System.currentTimeMillis();
		LinkedHashMap<String, SortOrder> sorts = new LinkedHashMap<>();
		BoolQueryBuilder bool = boolQuery();
		bool.must(new MatchQueryBuilder("geneToGeneParalogy.subjectGene.primaryExternalId", geneIds.get(0)));
		bool.filter(new TermQueryBuilder("category", "gene_to_gene_paralogy"));
		sorts.put("geneToGeneParalogy.rank", SortOrder.ASC);

		SearchResponse searchResponse = getSearchResponse(bool, pagination, sorts, false);
		
		List<GeneToGeneParalogyDocument> list = Arrays.stream(searchResponse.getHits().getHits())
			.map(searchHit -> {
				try {
					GeneToGeneParalogyDocument object = mapper.readValue(searchHit.getSourceAsString(), GeneToGeneParalogyDocument.class);
					return object;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}).toList();
		
		JsonResultResponse<GeneToGeneParalogyDocument> ret = new JsonResultResponse<>();
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);
		ret.setResults(list);
		ret.calculateRequestDuration(start);

		return ret;
	}
}
