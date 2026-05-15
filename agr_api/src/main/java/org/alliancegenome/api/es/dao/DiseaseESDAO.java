package org.alliancegenome.api.es.dao;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.document.es.DiseaseSummaryDocument;
import org.alliancegenome.api.es.dao.ESDAO;
import org.alliancegenome.core.es.util.EsClientFactory;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class DiseaseESDAO extends ESDAO {

	public static final String SITE_INDEX = ConfigHelper.getEsIndex();
	private static final String AGR_DO_SLIM = "DO_AGR_slim";
	private static final int AGR_SLIM_FETCH_SIZE = 200;

	private static volatile List<DiseaseSummaryDocument> agrSlimDocsCache;

	@Inject
	ObjectMapper mapper;

	public DiseaseSummaryDocument getById(String curie) {
		BoolQueryBuilder bool = boolQuery();
		bool.filter(new TermQueryBuilder("category", "disease_summary"));
		bool.filter(new TermQueryBuilder("doTerm.curie.keyword", curie));

		SearchSourceBuilder ssb = new SearchSourceBuilder();
		ssb.query(bool);
		ssb.size(1);

		SearchRequest searchRequest = new SearchRequest(SITE_INDEX);
		searchRequest.source(ssb);

		try {
			SearchResponse response = EsClientFactory.getDefaultEsClient().search(searchRequest, RequestOptions.DEFAULT);
			if (response.getHits().getHits().length == 0) {
				return null;
			}
			return mapper.readValue(response.getHits().getHits()[0].getSourceAsString(), DiseaseSummaryDocument.class);
		} catch (IOException e) {
			log.error("Failed to query disease_summary for curie=" + curie, e);
			return null;
		}
	}

	// Returns all disease_summary documents flagged as members of the AGR DO slim (doTerm.subsets contains "DO_AGR_slim"). Cached for the lifetime of the JVM since the curated slim list does not change at runtime.
	public List<DiseaseSummaryDocument> getAgrSlimDocs() {
		List<DiseaseSummaryDocument> cached = agrSlimDocsCache;
		if (cached != null) {
			return cached;
		}
		synchronized (DiseaseESDAO.class) {
			if (agrSlimDocsCache != null) {
				return agrSlimDocsCache;
			}
			agrSlimDocsCache = fetchAgrSlimDocs();
			return agrSlimDocsCache;
		}
	}

	private List<DiseaseSummaryDocument> fetchAgrSlimDocs() {
		BoolQueryBuilder bool = boolQuery();
		bool.filter(new TermQueryBuilder("category", "disease_summary"));
		bool.filter(new TermQueryBuilder("doTerm.subsets.keyword", AGR_DO_SLIM));

		SearchSourceBuilder ssb = new SearchSourceBuilder();
		ssb.query(bool);
		ssb.size(AGR_SLIM_FETCH_SIZE);

		SearchRequest searchRequest = new SearchRequest(SITE_INDEX);
		searchRequest.source(ssb);

		List<DiseaseSummaryDocument> docs = new ArrayList<>();
		try {
			SearchResponse response = EsClientFactory.getDefaultEsClient().search(searchRequest, RequestOptions.DEFAULT);
			for (SearchHit hit : response.getHits().getHits()) {
				docs.add(mapper.readValue(hit.getSourceAsString(), DiseaseSummaryDocument.class));
			}
		} catch (IOException e) {
			log.error("Failed to fetch AGR DO slim disease_summary docs", e);
		}
		return docs;
	}
}
