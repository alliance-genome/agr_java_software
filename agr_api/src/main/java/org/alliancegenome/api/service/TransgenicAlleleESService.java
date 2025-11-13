package org.alliancegenome.api.service;

import jakarta.enterprise.context.RequestScoped;
import org.alliancegenome.api.entity.GeneTransgenicAlleleSummaryDocument;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.es.model.query.Pagination;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


@RequestScoped
public class TransgenicAlleleESService extends ESService {

	public JsonResultResponse<GeneTransgenicAlleleSummaryDocument> getTransgenicAlleles(
		String geneId,
		Pagination pagination,
		boolean debug) {

		// unfiltered query
		BoolQueryBuilder query = getBaseModelQuery(List.of(geneId), false, "transgenic_allele_annotations");

		JsonResultResponse<GeneTransgenicAlleleSummaryDocument> ret = new JsonResultResponse<>();
		Map<String, String> aggregationFields = Map.of("alleleDocument.allele.taxon.name.keyword", "species");
		ret.setSupplementalData(getSupplementalData(geneId, true, debug, query, aggregationFields));

		// add table filter
		addTableFilter(pagination, query);
		LinkedHashMap<String, SortOrder> sortingMap = new LinkedHashMap<>();
		sortingMap.put("alleleDocument.phylogeneticSortingIndex", SortOrder.ASC);
		sortingMap.put("alleleDocument.allele.alleleSymbol.formatText.sort", SortOrder.ASC);

		SearchResponse searchResponse = getSearchResponse(query, pagination, sortingMap, null, debug);
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);

		List<GeneTransgenicAlleleSummaryDocument> list = Arrays.stream(searchResponse.getHits().getHits())
			.map(searchHit -> {
				try {
					GeneTransgenicAlleleSummaryDocument object = mapper.readValue(searchHit.getSourceAsString(), GeneTransgenicAlleleSummaryDocument.class);
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
