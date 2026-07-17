package org.alliancegenome.api.service;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;

import org.alliancegenome.core.document.ResourceDescriptorDocument;
import org.alliancegenome.api.es.dao.SearchDAO;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestScoped
public class ResourceDescriptorService {

	// Resource descriptors are a small, bounded set; one page large enough to hold them all.
	private static final int FETCH_LIMIT = 10000;

	@Inject ObjectMapper mapper;
	private static final SearchDAO searchDAO = new SearchDAO();

	public List<ResourceDescriptorDocument> getResourceDescriptors() {
		List<AggregationBuilder> aggBuilders = new ArrayList<>();
		HighlightBuilder hlb = new HighlightBuilder();
		LinkedHashMap<String, SortOrder> sorts = new LinkedHashMap<>();
		BoolQueryBuilder bool = boolQuery();
		bool.must(new MatchQueryBuilder("category", "resource_descriptor"));
		SearchResponse resp = searchDAO.performQuery(
			bool, aggBuilders, null, List.of("*"),
			FETCH_LIMIT, 0, hlb, sorts, false);

		if (resp == null || resp.getHits() == null) {
			log.error("Resource descriptor query returned no response from Elasticsearch");
			return new ArrayList<>();
		}

		long totalHits = resp.getHits().getTotalHits() != null ? resp.getHits().getTotalHits().value : 0;
		if (totalHits > FETCH_LIMIT) {
			log.warn("Resource descriptor query matched {} docs but only {} were returned — results truncated", totalHits, FETCH_LIMIT);
		}

		List<ResourceDescriptorDocument> results = new ArrayList<>();
		for (SearchHit hit : resp.getHits().getHits()) {
			try {
				results.add(mapper.readValue(hit.getSourceAsString(), ResourceDescriptorDocument.class));
			} catch (Exception e) {
				log.error("Failed to deserialize resource_descriptor document: {}", hit.getId(), e);
			}
		}
		// Deterministic order for a stable public listing (replaces an ordered YAML file).
		// Sorted client-side because `prefix` has no ES keyword sub-field to sort on server-side.
		results.sort(Comparator.comparing(ResourceDescriptorDocument::getPrefix, Comparator.nullsLast(Comparator.naturalOrder())));
		return results;
	}

}
