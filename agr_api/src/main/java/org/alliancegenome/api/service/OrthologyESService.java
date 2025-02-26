package org.alliancegenome.api.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

import org.alliancegenome.api.entity.GeneToGeneOrthologyDocument;
import org.alliancegenome.cache.repository.DiseaseCacheRepository;
import org.alliancegenome.cache.repository.ExpressionCacheRepository;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.cache.repository.helper.OrthologyCurationFiltering;
import org.alliancegenome.core.api.service.FilterService;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.es.index.site.dao.SearchDAO;
import org.alliancegenome.es.model.query.Pagination;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class OrthologyESService {

	@Inject ExpressionCacheRepository expressionCacheRepository;
	
	@Inject DiseaseCacheRepository diseaseCacheRepository;

	private static final SearchDAO searchDAO = new SearchDAO();

	private SearchResponse getSearchResponse(BoolQueryBuilder bool, Pagination pagination, LinkedHashMap<String, SortOrder> focusTaxonId, boolean debug) {
		List<AggregationBuilder> aggBuilders = new ArrayList<>();
		HighlightBuilder hlb = new HighlightBuilder();

		return searchDAO.performQuery(
			bool, aggBuilders, null, List.of("*"),
			pagination.getLimit(), pagination.getOffset(), hlb, focusTaxonId, debug);
	}

	public JsonResultResponse<GeneToGeneOrthologyDocument> getOrthologyList(String geneID, Pagination pagination) {
		BoolQueryBuilder bool = boolQuery();
		bool.filter(new TermQueryBuilder("geneToGeneOrthologyGenerated.subjectGene.primaryExternalId.keyword", geneID));
	
		SearchResponse searchResponse = getSearchResponse(bool, pagination, null, false);
	
		JsonResultResponse<GeneToGeneOrthologyDocument> response = new JsonResultResponse<>();
		response.setTotal((int) searchResponse.getHits().getTotalHits().value);

		
		List<GeneToGeneOrthologyDocument> list = new ArrayList<>();
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JavaTimeModule());
		mapper.registerModule(new Jdk8Module());
		mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
		mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
		mapper.setSerializationInclusion(JsonInclude.Include.NON_EMPTY);
		
		for (SearchHit searchHit : searchResponse.getHits().getHits()) {
			try {
				String source = searchHit.getSourceAsString();
				GeneToGeneOrthologyDocument object = mapper.readValue(source, GeneToGeneOrthologyDocument.class);
				list.add(object);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		FilterService<GeneToGeneOrthologyDocument> filterService = new FilterService<>(new OrthologyCurationFiltering());
		List<GeneToGeneOrthologyDocument> gene2GeneOrthoFiltered = filterService.filterAnnotations(list, pagination.getFieldFilterValueMap());

		Map<String, Object> map = new HashMap<>();

		gene2GeneOrthoFiltered.forEach(orthoDoc -> {
			putGeneInfo(map, orthoDoc.getGeneToGeneOrthologyGenerated().getSubjectGene());
			putGeneInfo(map, orthoDoc.getGeneToGeneOrthologyGenerated().getObjectGene());
		});
		
		response.setResults(gene2GeneOrthoFiltered);
		response.setSupplementalData(map);
		return response;
	}

	private void putGeneInfo(Map<String, Object> map, Gene gene) {
		Map<String, Object> data = new HashMap<>();
		data.put("taxonId", gene.getTaxon().getCurie());
		data.put("hasExpressionAnnotations", expressionCacheRepository.hasExpression(gene.getIdentifier()));
		data.put("hasDiseaseAnnotations", diseaseCacheRepository.hasDiseaseAnnotations(gene.getIdentifier()));
		map.put(gene.getIdentifier(), data);
	}
}
