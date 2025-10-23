package org.alliancegenome.api.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;
import static org.alliancegenome.cache.repository.helper.JsonResultResponse.DISTINCT_FIELD_VALUES;


import org.alliancegenome.curation_api.model.document.es.GeneExpressionDocument;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.apache.commons.collections4.CollectionUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.sort.SortOrder;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class ExpressionESService extends ESService {

	public JsonResultResponse<GeneExpressionDocument> getExpressionAnnotations(List<String> geneIDs, String termID, String focusTaxonId, Pagination pagination) {


		BoolQueryBuilder boolQuery = boolQuery();
		boolQuery.filter(new TermQueryBuilder("category", "gene_expression_annotation"));

		if (termID != null) {
			BoolQueryBuilder termQuery = boolQuery();
			termQuery.should(QueryBuilders.termQuery("termIds.keyword", termID));
			boolQuery.must(termQuery);
		}

		if (CollectionUtils.isNotEmpty(geneIDs)) {
			boolQuery.must(QueryBuilders.termsQuery("geneExpressionAnnotation.expressionAnnotationSubject.primaryExternalId.keyword", geneIDs));
		}

		JsonResultResponse<GeneExpressionDocument> result = new JsonResultResponse<>();
		result.setSupplementalData(getSupplementalData(boolQuery));

		addTableFilter(pagination, boolQuery);
		
		LinkedHashMap<String, SortOrder> sortOrders = getExpressionSortOrders(pagination, focusTaxonId);
		SearchResponse searchResponse = getSearchResponse(boolQuery, pagination, sortOrders, true);


		List<GeneExpressionDocument> list = new ArrayList<>();
		for (SearchHit searchHit : searchResponse.getHits().getHits()) {
			try {
				GeneExpressionDocument document = mapper.readValue(searchHit.getSourceAsString(), GeneExpressionDocument.class);
				list.add(document);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		result.setResults(list);
		result.setTotal((int) searchResponse.getHits().getTotalHits().value);

		return result;
	}
	
	private LinkedHashMap<String, SortOrder> getExpressionSortOrders(Pagination pagination, String focusTaxonId) {
		
		LinkedHashMap<String, SortOrder> sortingMap = new LinkedHashMap<>();
		LinkedHashMap<String, String> sortingSetMap = new LinkedHashMap<>();

		SpeciesType type = SpeciesType.getTypeByID(focusTaxonId);

		sortingSetMap.put("species", "geneExpressionAnnotation.expressionAnnotationSubject.taxon.name.keyword");
		sortingSetMap.put("gene", "geneExpressionAnnotation.expressionAnnotationSubject.geneSymbol.displayText.sort");
		sortingSetMap.put("location", "geneExpressionAnnotation.whereExpressedStatement.sort");
		sortingSetMap.put("stage", "geneExpressionAnnotation.whenExpressedStageName.sort");
		sortingSetMap.put("assay", "geneExpressionAnnotation.expressionAssayUsed.name.sort");
		sortingSetMap.put("default", "speciesOrder." + type.getTaxonIDPart());

		String sortField = sortingSetMap.get(pagination.getSortBy());

		if (sortField == null || pagination.getSortBy().equals("default")) {
			sortingMap.put(sortingSetMap.get("default"), SortOrder.ASC);
			sortingMap.put(sortingSetMap.get("location"), SortOrder.ASC);
			sortingMap.put(sortingSetMap.get("stage"), SortOrder.ASC);
		} else {
			sortingMap.put(sortField, SortOrder.ASC);
		}
		
		return sortingMap;
	}
	
	private Map<String, Object> getSupplementalData(BoolQueryBuilder unfilteredQuery) {

		Map<String, String> aggregationFields = new HashMap<>();
		aggregationFields.put("geneExpressionAnnotation.expressionAnnotationSubject.taxon.name.keyword", "species");
		aggregationFields.put("geneExpressionAnnotation.whereExpressedStatement.keyword", "location");
		aggregationFields.put("geneExpressionAnnotation.whenExpressedStageName.keyword", "stage");
		aggregationFields.put("geneExpressionAnnotation.expressionAssayUsed.name.keyword", "assay");

		Map<String, List<String>> distinctFieldValueMap = getAggregations(unfilteredQuery, aggregationFields, null, false, false);
		Map<String, Object> supplementalData = new LinkedHashMap<>();
		supplementalData.put(DISTINCT_FIELD_VALUES, distinctFieldValueMap);
		return supplementalData;
	}
}
