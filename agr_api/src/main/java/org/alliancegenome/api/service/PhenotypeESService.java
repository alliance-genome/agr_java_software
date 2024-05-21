package org.alliancegenome.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.logging.Log;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import org.alliancegenome.api.entity.GeneDiseaseAnnotationDocument;
import org.alliancegenome.api.entity.GenePhenotypeAnnotationDocument;
import org.alliancegenome.api.entity.PhenotypeAnnotationDocument;
import org.alliancegenome.api.service.helper.GeneDiseaseSearchHelper;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.api.service.DiseaseRibbonService;
import org.alliancegenome.es.index.site.dao.SearchDAO;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;

import java.util.*;

import static org.alliancegenome.cache.repository.helper.JsonResultResponse.DISTINCT_FIELD_VALUES;


@RequestScoped
public class PhenotypeESService extends ESService {

	@Inject
	ObjectMapper mapper;

	private static final GeneRepository geneRepository = new GeneRepository();
	private static final DiseaseRepository diseaseRepository = new DiseaseRepository();
	private static final DiseaseRibbonService diseaseRibbonService = new DiseaseRibbonService(diseaseRepository);
	private static final SearchDAO searchDAO = new SearchDAO();
	private static final GeneDiseaseSearchHelper geneDiseaseSearchHelper = new GeneDiseaseSearchHelper();

	// termID may be used in the future when converting disease page to new ES stack.
	public JsonResultResponse<GenePhenotypeAnnotationDocument> getGenePhenotypeAnnotations(String geneId,
																						   Pagination pagination,
																						   boolean debug) {

		// unfiltered query
		BoolQueryBuilder query = getBaseQuery(List.of(geneId), null, false, PhenotypeAnnotationDocument.GENE_PHENOTYPE_ANNOTATION, false);

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

/*
	private Map<String, Object> getSupplementalData(String focusTaxonId, boolean useSpeciesAggregation, boolean debug, BoolQueryBuilder unfilteredQuery) {
		// create histogram of select columns of unfiltered query
		Map<String, String> aggregationFields = new HashMap<>();
		if (useSpeciesAggregation) {
			aggregationFields.put("subject.taxon.name.keyword", "species");
		}
		aggregationFields.put("generatedRelationString.keyword", "associationType");
		aggregationFields.put("diseaseQualifiers.keyword", "diseaseQualifiers");
		Map<String, List<String>> distinctFieldValueMap = getAggregations(unfilteredQuery, aggregationFields, focusTaxonId, useSpeciesAggregation, debug);
		Map<String, Object> supplementalData = new LinkedHashMap<>();
		supplementalData.put(DISTINCT_FIELD_VALUES, distinctFieldValueMap);
		return supplementalData;
	}
*/

	private SearchResponse getSearchResponse(BoolQueryBuilder bool, Pagination pagination, LinkedHashMap<String, SortOrder> focusTaxonId, boolean debug) {
		List<AggregationBuilder> aggBuilders = new ArrayList<>();
		HighlightBuilder hlb = new HighlightBuilder();

		return searchDAO.performQuery(
			bool, aggBuilders, null, geneDiseaseSearchHelper.getResponseFields(),
			pagination.getLimit(), pagination.getOffset(), hlb, focusTaxonId, debug);
	}

	private LinkedHashMap<String, SortOrder> getAnnotationSorts(String focusTaxonId, boolean debug) {
		SpeciesType type = SpeciesType.getTypeByID(focusTaxonId);
		LinkedHashMap<String, SortOrder> sorts = new LinkedHashMap<>();
		if (type != null) {
			sorts.put("speciesOrder." + type.getTaxonIDPart(), SortOrder.ASC);
		} else {
			if (debug) {
				Log.info("Species could not be found for: " + focusTaxonId);
			} else {
				Log.debug("Species could not be found for: " + focusTaxonId);
			}
		}
		sorts.put("object.name.sort", SortOrder.ASC);
		if (debug) Log.info(sorts);
		return sorts;
	}

}
