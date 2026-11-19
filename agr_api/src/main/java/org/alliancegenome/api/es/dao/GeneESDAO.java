package org.alliancegenome.api.es.dao;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

import java.io.IOException;

import org.alliancegenome.core.es.util.EsClientFactory;
import org.alliancegenome.core.es.util.IndexName;
import org.alliancegenome.curation_api.model.document.es.GeneSummaryDocument;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@ApplicationScoped
public class GeneESDAO extends ESDAO {

	@Inject
	ObjectMapper mapper;

	public GeneSummaryDocument getById(String geneId) {
		BoolQueryBuilder bool = boolQuery();
		bool.filter(new TermQueryBuilder("category", "gene_summary"));
		bool.filter(new TermQueryBuilder("gene.primaryExternalId.keyword", geneId));

		SearchSourceBuilder ssb = new SearchSourceBuilder();
		ssb.query(bool);
		ssb.size(1);

		SearchRequest searchRequest = new SearchRequest(IndexName.SITE_INDEX.getIndexName());
		searchRequest.source(ssb);

		try {
			SearchResponse response = EsClientFactory.getDefaultEsClient().search(searchRequest, RequestOptions.DEFAULT);
			if (response.getHits().getHits().length == 0) {
				return null;
			}
			return mapper.readValue(response.getHits().getHits()[0].getSourceAsString(), GeneSummaryDocument.class);
		} catch (IOException e) {
			log.error("Failed to query gene_summary for id=" + geneId, e);
			return null;
		}
	}
}
