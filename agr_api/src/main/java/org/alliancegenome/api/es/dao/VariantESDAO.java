package org.alliancegenome.api.es.dao;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alliancegenome.core.es.util.EsClientFactory;
import org.alliancegenome.core.es.util.IndexName;
import org.alliancegenome.curation_api.model.document.es.VariantSummaryDocument;
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
public class VariantESDAO extends ESDAO {

	@Inject
	ObjectMapper mapper;

	private static Map<String, List<String>> sortAlleles = new HashMap<>();

	{
		sortAlleles.put("default", List.of("primaryKey.keyword"));
		sortAlleles.put("molecularConsequence", List.of("transcriptLevelConsequences.molecularConsequence.keyword", "primaryKey.keyword"));
		sortAlleles.put("variant", List.of("transcriptLevelConsequences.molecularConsequence.keyword", "primaryKey.keyword"));
	}

	public VariantSummaryDocument getVariant(String id) {

		BoolQueryBuilder bool = boolQuery();
		bool.filter(new TermQueryBuilder("category", "variant_summary"));
		bool.must(new TermQueryBuilder("variantList.curatedVariantGenomicLocations.hgvs.keyword", id));

		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(bool);

		SearchRequest searchRequest = new SearchRequest(IndexName.VARIANT_SUMMARY.getIndexName());
		searchRequest.source(searchSourceBuilder);
		SearchResponse response = null;

		try {
			response = EsClientFactory.getDefaultEsClient().search(searchRequest, RequestOptions.DEFAULT);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (response == null || response.getHits() == null || response.getHits().getHits().length == 0) {
			return null;
		}

		SearchHit[] searchHits = response.getHits().getHits();

		// Try to deserialize as VariantSummaryDocument (new curation API format)
		for (SearchHit hit : searchHits) {
			try {
				VariantSummaryDocument summaryDoc = mapper.readValue(hit.getSourceAsString(), VariantSummaryDocument.class);
				if (summaryDoc != null && summaryDoc.getVariantList() != null && !summaryDoc.getVariantList().isEmpty()) {
					return summaryDoc;
				}
			} catch (IOException e) {
				log.debug("Failed to deserialize as VariantSummaryDocument, trying AlleleVariantSequence: " + e.getMessage());
			}
		}
		return null;
	}
}
