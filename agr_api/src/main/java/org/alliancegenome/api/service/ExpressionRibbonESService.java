package org.alliancegenome.api.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.alliancegenome.api.dto.EntitySubgroupSlim;
import org.alliancegenome.api.dto.RibbonEntity;
import org.alliancegenome.api.dto.RibbonSection;
import org.alliancegenome.api.dto.RibbonSummary;
import org.alliancegenome.api.entity.SectionSlim;
import org.alliancegenome.cache.repository.ExpressionCacheRepository;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.core.util.FileHelper;
import org.alliancegenome.curation_api.model.document.es.GeneExpressionDocument;
import org.alliancegenome.curation_api.model.document.es.GeneExpressionRibbonSummaryDocument;
import org.alliancegenome.curation_api.model.entities.Gene;
import org.alliancegenome.curation_api.model.entities.ontology.GOTerm;
import org.alliancegenome.es.model.query.Pagination;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.sort.SortOrder;
import static org.elasticsearch.index.query.QueryBuilders.boolQuery;


import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestScoped
public class ExpressionRibbonESService extends ESService {

	@Inject ExpressionESService expressionService;

	public static final String UNDEFINED = "undefined";

	private static RibbonSummary ribbonSummary;

	public RibbonSummary getRibbonSectionInfo() {
		// get a deep clone of a template object
		// by serialization and deserialization (JSON)
		ObjectMapper objectMapper = new ObjectMapper();
		RibbonSummary deepCopy = null;
		try {
			deepCopy = objectMapper.readValue(objectMapper.writeValueAsString(getRibbonSections()), RibbonSummary.class);
		} catch (IOException e) {
			e.printStackTrace();
			log.error("Error: " + e);
		}

		return deepCopy;
	}

	public static Map<String, List<String>> slimParentTermIdMap = new LinkedHashMap<>();

	public static final String LOC_ALL = "LOC:ALL";

	public static final String STAGE_ALL = "STAGE:ALL";

	static {
		List<String> infection = new ArrayList<>();
		infection.add("Expression grouped by Locations");
		infection.add("All anatomical structures");
		slimParentTermIdMap.put(ExpressionCacheRepository.UBERON_ANATOMY_ROOT, infection);

		List<String> anatomy = new ArrayList<>();
		anatomy.add("Expression grouped by Stages");
		anatomy.add("All stages");
		slimParentTermIdMap.put(ExpressionCacheRepository.UBERON_STAGE_ROOT, anatomy);

		List<String> goTerms = new ArrayList<>();
		goTerms.add("Expression grouped by GO CC terms");
		goTerms.add("All cellular components");
		slimParentTermIdMap.put(ExpressionCacheRepository.GO_CC_ROOT, goTerms);

	}


	public GeneExpressionRibbonSummaryDocument getGeneExpressionRibbonSlimTerms() {

		BoolQueryBuilder boolQuery = boolQuery();
		boolQuery.filter(new TermQueryBuilder("category", "gene_expression_ribbon_summary"));
		LinkedHashMap<String, SortOrder> sorts = new LinkedHashMap<>();
		Pagination pagination = new Pagination();
		
		SearchResponse searchResponse = getSearchResponse(boolQuery, pagination, sorts, true);
		if (searchResponse.getHits().getHits().length > 0) {
			try {
				return mapper.readValue(searchResponse.getHits().getHits()[0].getSourceAsString(), GeneExpressionRibbonSummaryDocument.class);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return null;
	}

	public RibbonSummary getExpressionRibbonSummary(List<String> geneIDs) {
		if (geneIDs == null) {
			return null;
		}
		RibbonSummary ribbonSummary = getRibbonSectionInfo();
		geneIDs.forEach(geneID -> ribbonSummary.addRibbonEntity(getExpressionRibbonSummary(geneID)));
		return ribbonSummary;
	}

	private RibbonEntity getExpressionRibbonSummary(String geneID) {

		//List<ExpressionDetail> expressionList = expressionCacheRepository.getExpressionDetails(geneID);
		Pagination pagination = new Pagination(1, 250000, null, null);
		JsonResultResponse<GeneExpressionDocument> expressionAnnotations = expressionService.getExpressionAnnotations(List.of(geneID), null, null, pagination);

		Gene gene = expressionAnnotations.getResults().get(0).getGeneExpressionAnnotation().getExpressionAnnotationSubject();
		String dataProvider = expressionAnnotations.getResults().get(0).getGeneExpressionAnnotation().getDataProvider().getAbbreviation();
		RibbonEntity entity = new RibbonEntity();
		entity.setId(geneID);
		entity.setLabel(gene.getGeneSymbol().getDisplayText());
		entity.setTaxonID(gene.getTaxon().getCurie());
		entity.setTaxonName(gene.getTaxon().getName());

		// mark / add the 'not available' terms
		// Note: Stages are still handled separately than ao / go because they are modelled differently in the database.
		List<String> nonStageTerms = new ArrayList<>();
		getRibbonSections().getDiseaseRibbonSections().get(0).getSlims()
				.forEach(slim -> nonStageTerms.add(slim.getId()));
		getRibbonSections().getDiseaseRibbonSections().get(2).getSlims()
				.forEach(slim -> nonStageTerms.add(slim.getId()));
		List<String> stageTerms = new ArrayList<>();
		getRibbonSections().getDiseaseRibbonSections().get(1).getSlims()
				.forEach(slim -> stageTerms.add(slim.getId()));

		nonStageTerms.stream()
				.filter(id -> !entity.getSlims().keySet().contains(id))
				.forEach(id -> {
					EntitySubgroupSlim slim = getEntitySubgroupSlim(id, null, dataProvider);
					entity.addEntitySlim(slim);
				});
		stageTerms.stream()
				.filter(id -> !entity.getSlims().keySet().contains(id))
				.forEach(id -> {
					EntitySubgroupSlim slim = getEntitySubgroupStageSlim(id, null, dataProvider);
					entity.addEntitySlim(slim);
				});

		if (CollectionUtils.isEmpty(expressionAnnotations.getResults())) {
			return entity;
		}

		// create histograms for each of the three ontologies
		List<GeneExpressionDocument> uberonAnnotations = new ArrayList<>();
		MultiValuedMap<String, GeneExpressionDocument> aoUberonMap = new ArrayListValuedHashMap<>();

		List<GeneExpressionDocument> goAnnotations = new ArrayList<>();
		MultiValuedMap<String, GeneExpressionDocument> goTermMap = new ArrayListValuedHashMap<>();

		List<GeneExpressionDocument> stageAnnotations = new ArrayList<>();
		MultiValuedMap<String, GeneExpressionDocument> stageTermMap = new ArrayListValuedHashMap<>();

		expressionAnnotations.getResults().forEach(annotation -> {
			if (CollectionUtils.isNotEmpty(annotation.getUberonTermIds())) {
				uberonAnnotations.add(annotation);
				annotation.getUberonTermIds().forEach(uberonTerm -> aoUberonMap.put(uberonTerm, annotation));
			}

			if (CollectionUtils.isNotEmpty(annotation.getGoTermIds())) {
				goAnnotations.add(annotation);
				annotation.getGoTermIds().forEach(goTermId -> goTermMap.put(goTermId, annotation));
			}
			String stageTermID = null;
			if (CollectionUtils.isNotEmpty(annotation.getGeneExpressionAnnotation().getExpressionPattern().getWhenExpressed().getStageUberonSlimTerms())) {
				stageTermID = annotation.getGeneExpressionAnnotation().getExpressionPattern().getWhenExpressed().getStageUberonSlimTerms().get(0).getName();
			}
			if (stageTermID != null) {
				stageAnnotations.add(annotation);
				stageTermMap.put(stageTermID, annotation);
			}
		});

		

		// add the AO root term
		EntitySubgroupSlim slimRoot = getEntitySubgroupSlim(ExpressionCacheRepository.UBERON_ANATOMY_ROOT, uberonAnnotations, dataProvider);
		entity.addEntitySlim(slimRoot);
		aoUberonMap.keySet().forEach(uberonTermID -> {
			EntitySubgroupSlim slim = getEntitySubgroupSlim(uberonTermID, aoUberonMap.get(uberonTermID), dataProvider);
			entity.addEntitySlim(slim);
		});

		// add the Stage root term
		EntitySubgroupSlim slimRootStage = getEntitySubgroupStageSlim(ExpressionCacheRepository.UBERON_STAGE_ROOT, stageAnnotations, dataProvider);
		entity.addEntitySlim(slimRootStage);
		stageTermMap.keySet().forEach(uberonTermID -> {
			EntitySubgroupSlim slim = getEntitySubgroupStageSlim(uberonTermID, stageTermMap.get(uberonTermID), dataProvider);
			entity.addEntitySlim(slim);
		});

		// add the GO root term
		EntitySubgroupSlim slimRootGO = getEntitySubgroupSlim(ExpressionCacheRepository.GO_CC_ROOT, goAnnotations, dataProvider);
		entity.addEntitySlim(slimRootGO);
		goTermMap.keySet().forEach(goTermID -> {
			EntitySubgroupSlim slim = getEntitySubgroupSlim(goTermID, goTermMap.get(goTermID), dataProvider);
			entity.addEntitySlim(slim);
		});

		entity.setNumberOfClasses(getDistinctClassSize(expressionAnnotations.getResults()));
		entity.setNumberOfAnnotations(expressionAnnotations.getResults().size());

		return entity;
	}

		private EntitySubgroupSlim getEntitySubgroupSlim(String primaryKey, Collection<GeneExpressionDocument> aoAnnotations, String dataProvider) {
		EntitySubgroupSlim slim = new EntitySubgroupSlim();
		slim.setId(primaryKey);
		if (aoAnnotations != null) {
			slim.setNumberOfAnnotations(aoAnnotations.size());
			slim.setNumberOfClasses(getDistinctClassSize(aoAnnotations));
		}
		slim.setAvailable(FileHelper.getRibbonTermSpeciesApplicability(primaryKey, dataProvider));
		return slim;
	}

	private int getDistinctClassSize(Collection<GeneExpressionDocument> aoAnnotations) {
		return aoAnnotations.stream().collect(Collectors.groupingBy(e -> e.getGeneExpressionAnnotation().getWhereExpressedStatement())).size();
	}

	private EntitySubgroupSlim getEntitySubgroupStageSlim(String primaryKey, Collection<GeneExpressionDocument> stageAnnotations, String dataProvider) {
		EntitySubgroupSlim slim = new EntitySubgroupSlim();
		slim.setId(primaryKey);
		if (stageAnnotations != null) {
			slim.setNumberOfAnnotations(stageAnnotations.size());
			slim.setNumberOfClasses(getDistinctStageClassSize(stageAnnotations));
		}
		slim.setAvailable(FileHelper.getRibbonTermSpeciesApplicability(primaryKey, dataProvider));
		return slim;
	}

	private int getDistinctStageClassSize(Collection<GeneExpressionDocument> stageAnnotations) {
		return stageAnnotations.stream().map(e -> e.getGeneExpressionAnnotation().getWhenExpressedStageName()).collect(Collectors.toSet()).size();
	}

	public RibbonSummary getRibbonSections() {
		if (ribbonSummary != null) {
			return ribbonSummary;
		}

		ribbonSummary = new RibbonSummary();

		GeneExpressionRibbonSummaryDocument slimTerms = getGeneExpressionRibbonSlimTerms();

		slimParentTermIdMap.forEach((id, names) -> {
			RibbonSection section = new RibbonSection();
			section.setLabel(names.get(0));
			section.setId(id);
			
			SectionSlim allSlimElement = new SectionSlim();
			allSlimElement.setId(id);
			allSlimElement.setLabel(names.get(1));
			allSlimElement.setTypeAll();
			section.addDiseaseSlim(allSlimElement);
			ribbonSummary.addRibbonSection(section);

			List<GOTerm> goSlimList = slimTerms.getGoSlimTerms();
			if (id.equals(ExpressionCacheRepository.GO_CC_ROOT)) {
				goSlimList.forEach(term -> {
					if (term.getCurie().equals(ExpressionCacheRepository.GO_CC_ROOT)) {
						section.setDescription(term.getDefinition());
						allSlimElement.setDescription(term.getDefinition());
					} else {
						SectionSlim slim = getSectionSlim(term.getCurie(), term.getName(), term.getDefinition());
						section.addDiseaseSlim(slim);
					}
				});
			}
			if (id.equals(ExpressionCacheRepository.UBERON_ANATOMY_ROOT)) {
				slimTerms.getAnatomicalStructureSlimTerms().forEach(term -> {
					if (term.getCurie().equals(ExpressionCacheRepository.UBERON_ANATOMY_ROOT)) {
						section.setDescription(term.getDefinition());
						allSlimElement.setDescription(term.getDefinition());
					} else {
						SectionSlim slim = getSectionSlim(term.getCurie(), term.getName(), term.getDefinition());
						section.addDiseaseSlim(slim);
					}
				});
			}
			if (id.equals(ExpressionCacheRepository.UBERON_STAGE_ROOT)) {
				slimTerms.getStageSlimTerms().forEach(term -> {
					if (term.getCurie().equals(ExpressionCacheRepository.UBERON_STAGE_ROOT)) {
						section.setDescription(term.getDefinition());
						allSlimElement.setDescription(term.getDefinition());
					} else {
						SectionSlim slim = getSectionSlim(term.getCurie(), term.getName(), term.getDefinition());
						section.addDiseaseSlim(slim);
					}
				});
			}
		});

		return ribbonSummary;
	}

	private SectionSlim getSectionSlim(String primaryKey, String name, String def) {
		SectionSlim slim = new SectionSlim();
		slim.setId(primaryKey);
		slim.setLabel(name);
		if (def == null) {
			def = UNDEFINED;
		}
		slim.setDescription(def);
		return slim;
	}

}
