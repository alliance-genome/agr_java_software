package org.alliancegenome.api.service;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.alliancegenome.api.entity.ReleaseInfoDocument;
import org.alliancegenome.curation_api.model.input.Pagination;
import org.alliancegenome.es.index.site.dao.SearchDAO;
import org.alliancegenome.neo4j.entity.ReleaseSummary;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class ReleaseInfoService {

	@Inject ObjectMapper mapper;
	private static final SearchDAO searchDAO = new SearchDAO();

	public ReleaseInfoDocument getReleaseInfo() {
		List<AggregationBuilder> aggBuilders = new ArrayList<>();
		HighlightBuilder hlb = new HighlightBuilder();
		LinkedHashMap<String, SortOrder> sorts = new LinkedHashMap<>();
		Pagination pagination = new Pagination();
		BoolQueryBuilder bool = boolQuery();
		bool.must(new MatchQueryBuilder("category", "release_info"));
		SearchResponse resp = searchDAO.performQuery(
			bool, aggBuilders, null, List.of("*"),
			pagination.getLimit(), pagination.getPage() * pagination.getLimit(), hlb, sorts, false);

		if (resp.getHits().getHits().length > 0) {
			try {
				return mapper.readValue(resp.getHits().getHits()[0].getSourceAsString(), ReleaseInfoDocument.class);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public ReleaseSummary getSummary() {
		ReleaseSummary sum = new ReleaseSummary();
		// sum.setReleaseInfo(StreamSupport.stream(releaseRepo.getAll().spliterator(), false).collect(Collectors.toList()).get(0));
		// sum.setMetaData(StreamSupport.stream(modFileRepo.getAll().spliterator(), false).collect(Collectors.toList()));
		// sum.setOntologyMetaData(StreamSupport.stream(ontologyFileRepo.getAll().spliterator(), false).collect(Collectors.toList()));
		return sum;
	}

}
