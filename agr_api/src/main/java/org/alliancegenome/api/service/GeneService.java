package org.alliancegenome.api.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.cache.*;
import org.alliancegenome.cache.repository.*;
import org.alliancegenome.cache.repository.helper.*;
import org.alliancegenome.core.variant.service.AlleleVariantIndexService;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.*;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.repository.*;
import org.apache.commons.collections.CollectionUtils;

@RequestScoped
public class GeneService {

    private static GeneRepository geneRepo = new GeneRepository();
    private static InteractionRepository interRepo = new InteractionRepository();
    private static PhenotypeRepository phenoRepo = new PhenotypeRepository();

    @Inject
    private InteractionCacheRepository interCacheRepo;

    @Inject
    private PhenotypeCacheRepository phenoCacheRepo;

    @Inject
    private AlleleService alleleService;

    @Inject
    private AlleleVariantIndexService alleleVariantIndexService;

    @Inject
    private GeneCacheRepository geneCacheRepo;

    @Inject
    private AlleleCacheRepository alleleCacheRepository;

    public Gene getById(String id) {
        Gene gene = geneRepo.getOneGene(id);
        // if not found directly check if it is a secondary id on a different gene
        if (gene == null) {
            return geneRepo.getOneGeneBySecondaryId(id);
        }
        return gene;
    }

    public JsonResultResponse<Allele> getAlleles(String geneId, Pagination pagination) {
        long startTime = System.currentTimeMillis();
        List<Allele> alleles=   alleleVariantIndexService.getAlleles(geneId);
        JsonResultResponse<Allele> response=alleleCacheRepository .getAlleleJsonResultResponse(pagination, alleles);
        if (response == null)
            response = new JsonResultResponse<>();
        long duration = (System.currentTimeMillis() - startTime) / 1000;
        response.setRequestDuration(Long.toString(duration));
        return response;
    }

    public JsonResultResponse<AlleleVariantSequence> getAllelesAndVariantInfo(String geneId, Pagination pagination) {
        List<AlleleVariantSequence> allelesNVariants = alleleVariantIndexService.getAllelesNVariants(geneId);
        if (allelesNVariants == null)
            return null;
        return alleleCacheRepository.getAlleleAndVariantJsonResultResponse(pagination, allelesNVariants);
    }

    public JsonResultResponse<InteractionGeneJoin> getInteractions(String id, Pagination pagination, String joinType) {
        JsonResultResponse<InteractionGeneJoin> response = new JsonResultResponse<>();
        PaginationResult<InteractionGeneJoin> interactions = interCacheRepo.getInteractionAnnotationList(id, pagination, joinType);
        response.addAnnotationSummarySupplementalData(getInteractionSummary(id));
        if (interactions == null)
            return response;
        //FilterService<InteractionGeneJoin> filterService = new FilterService<>(new InteractionAnnotationFiltering());
        //ColumnFieldMapping<InteractionGeneJoin> mapping = new InteractionColumnFieldMapping();
        //List<InteractionGeneJoin> interactionAnnotationList = geneCacheRepo.getInteractions(id);
        //response.addDistinctFieldValueSupplementalData(filterService.getDistinctFieldValues(interactionAnnotationList,
        //        mapping.getSingleValuedFieldColumns(Table.INTERACTION), mapping));        
        response.addDistinctFieldValueSupplementalData(interactions.getDistinctFieldValueMap());
        response.setResults(interactions.getResult());
        response.setTotal(interactions.getTotalNumber());
        return response;
    }
    public JsonResultResponse<InteractionGeneJoin> getInteractions(String id, Pagination pagination) {
        return getInteractions(id, pagination, "");
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
        if(CollectionUtils.isEmpty(taxonIDs))
            return null;
        List<String> taxIDs = taxonIDs.stream()
                .map(SpeciesType::getTaxonId)
                .collect(Collectors.toList());
        return geneRepo.getAllGenes(taxIDs);
    }
}
