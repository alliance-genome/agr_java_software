package org.alliancegenome.api.service;

import jakarta.enterprise.context.RequestScoped;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.curation_api.model.document.es.AffectedGenomicModelDocument;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.es.util.EsClientFactory;
import org.elasticsearch.action.search.SearchRequest;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.script.Script;
import org.elasticsearch.script.ScriptType;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.ScriptSortBuilder;
import org.elasticsearch.search.sort.SortBuilders;
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
		sortingSetMap.put("default", List.of(
			"hasDiseaseAnnotations.keyword",
			"hasPhenotypeAnnotations.keyword",
			"model.agmFullName.formatText.keyword",
			"diseaseTerms.name.keyword"));
		LinkedHashMap<String, SortOrder> sortingMap = new LinkedHashMap<>();

		List<String> sortFields = sortingSetMap.get(pagination.getSortBy());
		if (sortFields == null) {
			sortFields = sortingSetMap.get("default");
		}

		// Create search source builder for custom script sorting
		SearchSourceBuilder searchSourceBuilder = new SearchSourceBuilder();
		searchSourceBuilder.query(query);
		searchSourceBuilder.from(pagination.getStart());
		searchSourceBuilder.size(pagination.getLimit());

		// Handle sorting with case-insensitive script for model name field
		for (String sortField : sortFields) {
			if ("model.agmFullName.formatText.keyword".equals(sortField)) {
				// Use script for case-insensitive sorting
				Script script = new Script(ScriptType.INLINE, "painless",
					"if (doc['model.agmFullName.formatText.keyword'].size() > 0) { "
					+"doc['model.agmFullName.formatText.keyword'].value.toLowerCase() } else { '' }",
					Collections.emptyMap());
				ScriptSortBuilder scriptSort = SortBuilders.scriptSort(script, ScriptSortBuilder.ScriptSortType.STRING);
				scriptSort.order(SortOrder.ASC);
				searchSourceBuilder.sort(scriptSort);
			} else {
				searchSourceBuilder.sort(sortField, SortOrder.ASC);
			}
		}

		SearchRequest searchRequest = new SearchRequest(ConfigHelper.getEsIndex());
		searchRequest.source(searchSourceBuilder);
		SearchResponse searchResponse;
		try {
			searchResponse = EsClientFactory.getDefaultEsClient().search(searchRequest, RequestOptions.DEFAULT);
		} catch (Exception e) {
			throw new RuntimeException("Error executing search", e);
		}
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
