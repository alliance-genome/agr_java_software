package org.alliancegenome.api.service;

import static org.elasticsearch.index.query.QueryBuilders.boolQuery;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.api.entity.GeneGeneticInteractionDocument;
import org.alliancegenome.api.entity.GeneMolecularInteractionDocument;
import org.alliancegenome.api.service.helper.ElasticSearchHelper;
import org.alliancegenome.cache.repository.AlleleCacheRepository;
import org.alliancegenome.cache.repository.PhenotypeCacheRepository;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.cache.repository.helper.PaginationResult;
import org.alliancegenome.core.variant.service.AlleleVariantIndexService;
import org.alliancegenome.es.index.site.dao.SearchDAO;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.EntitySummary;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.BioEntityGeneExpressionJoin;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.repository.InteractionRepository;
import org.alliancegenome.neo4j.repository.PhenotypeRepository;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.MatchQueryBuilder;
import org.elasticsearch.index.query.TermQueryBuilder;
import org.elasticsearch.search.aggregations.AggregationBuilder;
import org.elasticsearch.search.fetch.subphase.highlight.HighlightBuilder;
import org.elasticsearch.search.sort.SortOrder;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

@RequestScoped
public class GeneService {

	private static GeneRepository geneRepo = new GeneRepository();
	private static InteractionRepository interRepo = new InteractionRepository();
	private static PhenotypeRepository phenoRepo = new PhenotypeRepository();
	private static final ElasticSearchHelper elasticSearchHelper = new ElasticSearchHelper();
	private static final SearchDAO searchDAO = new SearchDAO();
	
	@Inject AlleleVariantIndexService alleleVariantIndexService;
	
	@Inject AlleleCacheRepository alleleCacheRepository;

	@Inject PhenotypeCacheRepository phenoCacheRepo;
	
	@Inject ObjectMapper mapper;

	public Gene getById(String id) {
		Gene gene = geneRepo.getOneGene(id);
		// if not found directly check if it is a secondary id on a different gene
		if (gene == null) {
			return geneRepo.getOneGeneBySecondaryId(id);
		}
		return gene;
	}
	
	public List<BioEntityGeneExpressionJoin> getExpressionAnnotationsByTaxon(String taxon, String termID, Pagination pagination) {
		return geneRepo.getExpressionAnnotationsByTaxon(taxon, termID, pagination);
	}

	public JsonResultResponse<Allele> getAlleles(String geneId, Pagination pagination) {
		long startTime = System.currentTimeMillis();

		JsonResultResponse<Allele> response = alleleVariantIndexService.getAlleles(geneId, pagination); // This needs to be a Helper function
		if (response == null) {
			response = new JsonResultResponse<>();
		}
		long duration = (System.currentTimeMillis() - startTime) / 1000;
		response.setRequestDuration(Long.toString(duration));
		return response;
	}

	public JsonResultResponse<AlleleVariantSequence> getAllelesAndVariantInfo(String geneId, Pagination pagination) {
		List<AlleleVariantSequence> allelesNVariants = alleleVariantIndexService.getAllelesNVariants(geneId, pagination);
		if (allelesNVariants == null) {
			return null;
		}
		return alleleCacheRepository.getAlleleAndVariantJsonResultResponse(pagination, allelesNVariants);
	}
	
	public JsonResultResponse<GeneGeneticInteractionDocument> getGeneticInteractions(String geneId, Pagination pagination) {
		BoolQueryBuilder query = boolQuery();
		query.should(new MatchQueryBuilder("geneGeneticInteraction.geneAssociationSubject.curie.keyword", geneId));
		query.should(new MatchQueryBuilder("geneGeneticInteraction.geneAssociationSubject.primaryExternalId.keyword", geneId));
		query.should(new MatchQueryBuilder("geneGeneticInteraction.geneAssociationSubject.modInternalId.keyword", geneId));

		JsonResultResponse<GeneGeneticInteractionDocument> ret = new JsonResultResponse<>();
		//ret.setSupplementalData(getSupplementalData(null, false, debug, query));

		// add table filter
		elasticSearchHelper.addTableFilter(pagination, query);
		
		LinkedHashMap<String, SortOrder> sorts = new LinkedHashMap<>();
		if (StringUtils.isNotBlank(pagination.getSortBy())) {
			sorts.put(pagination.getSortBy(), SortOrder.ASC);
		}
		
		List<AggregationBuilder> aggBuilders = new ArrayList<>();
		HighlightBuilder hlb = new HighlightBuilder();
		SearchResponse searchResponse = searchDAO.performQuery(query, aggBuilders, null, List.of("*"), pagination.getLimit(), pagination.getOffset(), hlb, sorts, false);
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);

		List<GeneGeneticInteractionDocument> list = Arrays.stream(searchResponse.getHits().getHits())
			.map(searchHit -> {
				try {
					GeneGeneticInteractionDocument object = mapper.readValue(searchHit.getSourceAsString(), GeneGeneticInteractionDocument.class);
					return object;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}).toList();
		ret.setResults(list);
		return ret;

	}
	
	public JsonResultResponse<GeneMolecularInteractionDocument> getMolecularInteractions(String geneId, Pagination pagination) {
		BoolQueryBuilder query = boolQuery();
		BoolQueryBuilder query2 = boolQuery();
		query.must(query2);
		query2.should(new MatchQueryBuilder("geneMolecularInteraction.geneAssociationSubject.curie.keyword", geneId));
		query2.should(new MatchQueryBuilder("geneMolecularInteraction.geneAssociationSubject.primaryExternalId.keyword", geneId));
		query2.should(new MatchQueryBuilder("geneMolecularInteraction.geneAssociationSubject.modInternalId.keyword", geneId));

		query.filter(new TermQueryBuilder("category", "gene_molecular_interaction"));
		JsonResultResponse<GeneMolecularInteractionDocument> ret = new JsonResultResponse<>();
		//ret.setSupplementalData(getSupplementalData(null, false, debug, query));

		// add table filter
		elasticSearchHelper.addTableFilter(pagination, query);
		
		LinkedHashMap<String, SortOrder> sorts = new LinkedHashMap<>();
		if (StringUtils.isNotBlank(pagination.getSortBy())) {
			sorts.put(pagination.getSortBy(), SortOrder.ASC);
		}
		
		List<AggregationBuilder> aggBuilders = new ArrayList<>();
		HighlightBuilder hlb = new HighlightBuilder();
		SearchResponse searchResponse = searchDAO.performQuery(query, aggBuilders, null, List.of("*"), pagination.getLimit(), pagination.getOffset(), hlb, sorts, false);
		ret.setTotal((int) searchResponse.getHits().getTotalHits().value);

		List<GeneMolecularInteractionDocument> list = Arrays.stream(searchResponse.getHits().getHits())
			.map(searchHit -> {
				try {
					GeneMolecularInteractionDocument object = mapper.readValue(searchHit.getSourceAsString(), GeneMolecularInteractionDocument.class);
					return object;
				} catch (Exception e) {
					e.printStackTrace();
				}
				return null;
			}).toList();
		ret.setResults(list);
		return ret;

	}

	public JsonResultResponse<PhenotypeAnnotation> getPhenotypeAnnotations(String geneID, Pagination pagination) {
		LocalDateTime startDate = LocalDateTime.now();
		PaginationResult<PhenotypeAnnotation> list = phenoCacheRepo.getPhenotypeAnnotationList(geneID, pagination);
		JsonResultResponse<PhenotypeAnnotation> response = new JsonResultResponse<>();
		response.calculateRequestDuration(startDate);
		response.setResults(list.getResult());
		response.setTotal(list.getTotalNumber());
		return response;
	}

	public EntitySummary getPhenotypeSummary(String geneID) {
		EntitySummary summary = new EntitySummary();
		summary.setNumberOfAnnotations(phenoRepo.getTotalPhenotypeCount(geneID, new Pagination()));
		summary.setNumberOfEntities(phenoRepo.getDistinctPhenotypeCount(geneID));
		return summary;
	}

	public EntitySummary getInteractionSummary(String geneID) {
		EntitySummary summary = new EntitySummary();
		summary.setNumberOfAnnotations(interRepo.getInteractionCount(geneID));
		summary.setNumberOfEntities(interRepo.getInteractorCount(geneID));
		return summary;
	}

	public List<Gene> getAllGenes(List<String> species) {
		List<String> taxonIDs;
		if (CollectionUtils.isEmpty(species)) {
			taxonIDs = SpeciesType.getAllTaxonIDList();
		} else {
			taxonIDs = species.stream()
					.map(SpeciesType::getTaxonId)
					.collect(Collectors.toList());
		}
		if (CollectionUtils.isEmpty(taxonIDs)) {
			return null;
		}
		List<String> taxIDs = taxonIDs.stream()
				.map(SpeciesType::getTaxonId)
				.collect(Collectors.toList());
		return geneRepo.getAllGenes(taxIDs);
	}

}