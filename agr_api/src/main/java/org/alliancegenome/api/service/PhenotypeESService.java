package org.alliancegenome.api.service;

import jakarta.enterprise.context.RequestScoped;
import org.alliancegenome.api.entity.AllelePhenotypeAnnotationDocument;
import org.alliancegenome.api.entity.GenePhenotypeAnnotationDocument;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.es.model.query.Pagination;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.util.*;


@RequestScoped
public class PhenotypeESService extends ESService {

	// termID may be used in the future when converting disease page to new ES stack.
	public JsonResultResponse<GenePhenotypeAnnotationDocument> getGenePhenotypeAnnotations(
		String geneId,
		Pagination pagination,
		boolean debug) {

		// unfiltered query
		BoolQueryBuilder query = getBaseQuery(List.of(geneId), null, false, GenePhenotypeAnnotationDocument.GENE_PHENOTYPE_ANNOTATION, false);

		JsonResultResponse<GenePhenotypeAnnotationDocument> ret = new JsonResultResponse<>();

		// add table filter
		addTableFilter(pagination, query);
		// Sorting sets for different names of the sorting selection box
		Map<String, List<String>> sortingSetMap = new HashMap<>();
		sortingSetMap.put("default", List.of("phenotypeStatement.sort"));
		LinkedHashMap<String, SortOrder> sortingMap = new LinkedHashMap<>();

		List<String> sortFields = sortingSetMap.get(pagination.getSortBy());
		if (sortFields == null) {
			sortFields = sortingSetMap.get("default");
		}
		sortFields.forEach(sortField -> sortingMap.put(sortField, SortOrder.ASC));

		SearchResponse searchResponse = getSearchResponse(query, pagination, sortingMap, debug);
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);

		List<GenePhenotypeAnnotationDocument> list = Arrays.stream(searchResponse.getHits().getHits())
			.map(searchHit -> {
				try {
					GenePhenotypeAnnotationDocument object = mapper.readValue(searchHit.getSourceAsString(), GenePhenotypeAnnotationDocument.class);
					object.setUniqueId(searchHit.getId());
					return object;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}).toList();
		ret.setResults(list);
		return ret;
	}

	public JsonResultResponse<AllelePhenotypeAnnotationDocument> getAllelePhenotypeAnnotations(
		String alleleId,
		Pagination pagination,
		boolean debug) {

		// unfiltered query
		BoolQueryBuilder query = getBaseQuery(List.of(alleleId), null, false, AllelePhenotypeAnnotationDocument.ALLELE_PHENOTYPE_ANNOTATION, false);

		JsonResultResponse<AllelePhenotypeAnnotationDocument> ret = new JsonResultResponse<>();

		// add table filter
		addTableFilter(pagination, query);
		// Sorting sets for different names of the sorting selection box
		Map<String, List<String>> sortingSetMap = new HashMap<>();
		sortingSetMap.put("default", List.of("phenotypeStatement.sort"));
		LinkedHashMap<String, SortOrder> sortingMap = new LinkedHashMap<>();

		List<String> sortFields = sortingSetMap.get(pagination.getSortBy());
		if (sortFields == null) {
			sortFields = sortingSetMap.get("default");
		}
		sortFields.forEach(sortField -> sortingMap.put(sortField, SortOrder.ASC));

		SearchResponse searchResponse = getSearchResponse(query, pagination, sortingMap, debug);
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);

		List<AllelePhenotypeAnnotationDocument> list = Arrays.stream(searchResponse.getHits().getHits())
			.map(searchHit -> {
				try {
					AllelePhenotypeAnnotationDocument object = mapper.readValue(searchHit.getSourceAsString(), AllelePhenotypeAnnotationDocument.class);
					object.setUniqueId(searchHit.getId());
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
