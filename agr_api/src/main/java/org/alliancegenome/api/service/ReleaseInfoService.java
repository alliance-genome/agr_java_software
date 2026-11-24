package org.alliancegenome.api.service;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.alliancegenome.api.es.dao.SearchDAO;
import org.alliancegenome.core.document.ReleaseInfoDocument;
import org.alliancegenome.core.es.util.IndexName;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class ReleaseInfoService {

	@Inject ObjectMapper mapper;
	private static final SearchDAO searchDAO = new SearchDAO();
	private static ReleaseInfoDocument releaseInfoDocument;

	public ReleaseInfoDocument getReleaseInfo() {
		if(releaseInfoDocument != null) {
			return releaseInfoDocument;
		}

		BoolQueryBuilder bool = boolQuery();
		bool.must(new MatchQueryBuilder("category", "release_info"));
		SearchResponse resp = searchDAO.performQuery(IndexName.SITE_INDEX, bool, new ArrayList<>(), null, List.of("*"), 1, 0, new HighlightBuilder(), new LinkedHashMap<>(), false);

		if (resp.getHits().getHits().length > 0) {
			try {
				releaseInfoDocument = mapper.readValue(resp.getHits().getHits()[0].getSourceAsString(), ReleaseInfoDocument.class);
				return releaseInfoDocument;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

}
