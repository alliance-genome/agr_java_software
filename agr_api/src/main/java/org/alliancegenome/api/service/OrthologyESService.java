package org.alliancegenome.api.service;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alliancegenome.api.entity.GeneToGeneOrthologyDocument;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.es.model.query.FieldFilter;
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

		String stringency = pagination.getFieldFilterValueMap().get(FieldFilter.STRINGENCY);
		if (stringency != null && !stringency.equalsIgnoreCase("all")) {
			bool.filter(new TermQueryBuilder("stringencyFilter.keyword", stringency.toLowerCase()));
		}

		SearchResponse searchResponse = getSearchResponse(bool, pagination, null, false);

		JsonResultResponse<GeneToGeneOrthologyDocument> response = new JsonResultResponse<>();
		response.setTotal((int) searchResponse.getHits().getTotalHits().value);

		List<GeneToGeneOrthologyDocument> list = new ArrayList<>();

		for (SearchHit searchHit : searchResponse.getHits().getHits()) {
			try {
				String source = searchHit.getSourceAsString();
				GeneToGeneOrthologyDocument doc = mapper.readValue(source, GeneToGeneOrthologyDocument.class);

				Map<String, Map<String, Object>> geneAnnotationsMap = new HashMap<>();
				doc.setGeneAnnotationsMap(geneAnnotationsMap);
				for (Map<String, Object> geneAnnotation : doc.getGeneAnnotations()) {
					String geneIdentifier = (String) geneAnnotation.get("geneIdentifier");
					geneAnnotationsMap.put(geneIdentifier, geneAnnotation);
				}

				list.add(doc);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

		response.setResults(list);
		return response;
	}


}
