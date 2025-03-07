package org.alliancegenome.api.service;

import java.util.ArrayList;
import java.util.List;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

import org.alliancegenome.api.entity.GeneToGeneOrthologyDocument;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.cache.repository.helper.OrthologyFiltering;
import org.alliancegenome.core.api.service.FilterService;
import org.alliancegenome.es.model.query.Pagination;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;

import jakarta.enterprise.context.RequestScoped;

@RequestScoped
public class OrthologyESService extends ESService {

	public JsonResultResponse<GeneToGeneOrthologyDocument> getOrthologyList(String geneID, Pagination pagination) {
		BoolQueryBuilder bool = boolQuery();
		bool.filter(new TermQueryBuilder("geneToGeneOrthologyGenerated.subjectGene.primaryExternalId.keyword", geneID));
	
		SearchResponse searchResponse = getSearchResponse(bool, pagination, null, false);
	
		JsonResultResponse<GeneToGeneOrthologyDocument> response = new JsonResultResponse<>();
		response.setTotal((int) searchResponse.getHits().getTotalHits().value);

		List<GeneToGeneOrthologyDocument> list = new ArrayList<>();
		
		for (SearchHit searchHit : searchResponse.getHits().getHits()) {
			try {
				String source = searchHit.getSourceAsString();
				GeneToGeneOrthologyDocument object = mapper.readValue(source, GeneToGeneOrthologyDocument.class);
				list.add(object);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		FilterService<GeneToGeneOrthologyDocument> filterService = new FilterService<>(new OrthologyFiltering());
		List<GeneToGeneOrthologyDocument> gene2GeneOrthoFiltered = filterService.filterAnnotations(list, pagination.getFieldFilterValueMap());
		
		response.setResults(gene2GeneOrthoFiltered);
		return response;
	}


}
