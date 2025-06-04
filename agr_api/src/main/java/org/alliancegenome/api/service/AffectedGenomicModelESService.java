package org.alliancegenome.api.service;

import jakarta.enterprise.context.RequestScoped;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.curation_api.model.document.es.AffectedGenomicModelDocument;
import org.alliancegenome.es.model.query.Pagination;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.util.*;


@RequestScoped
public class AffectedGenomicModelESService extends ESService {

	public JsonResultResponse<AffectedGenomicModelDocument> getGeneModels(
		String geneId,
		Pagination pagination,
		boolean debug) {

		// unfiltered query
		BoolQueryBuilder query = getBaseModelQuery(List.of(geneId), false, "affected_genomic_model_annotation");

		JsonResultResponse<AffectedGenomicModelDocument> ret = new JsonResultResponse<>();

		// add table filter
		addTableFilter(pagination, query);
		// Sorting sets for different names of the sorting selection box
		Map<String, List<String>> sortingSetMap = new HashMap<>();
		sortingSetMap.put("default", List.of("model.agmFullName.formatText.keyword"));
		LinkedHashMap<String, SortOrder> sortingMap = new LinkedHashMap<>();

		List<String> sortFields = sortingSetMap.get(pagination.getSortBy());
		if (sortFields == null) {
			sortFields = sortingSetMap.get("default");
		}
		sortFields.forEach(sortField -> sortingMap.put(sortField, SortOrder.ASC));

		SearchResponse searchResponse = getSearchResponse(query, pagination, sortingMap, debug);
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);

		List<AffectedGenomicModelDocument> list = Arrays.stream(searchResponse.getHits().getHits())
			.map(searchHit -> {
				try {
					AffectedGenomicModelDocument object = mapper.readValue(searchHit.getSourceAsString(), AffectedGenomicModelDocument.class);
					return object;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}).toList();
		ret.setResults(list);
		return ret;
	}

}
