package org.alliancegenome.api.service;

import org.alliancegenome.cache.repository.InteractionCacheRepository;
import org.alliancegenome.cache.repository.PhenotypeCacheRepository;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.PaginationResult;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.EntitySummary;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.repository.InteractionRepository;
import org.alliancegenome.neo4j.repository.PhenotypeRepository;

import javax.enterprise.context.RequestScoped;
import java.time.LocalDateTime;

@RequestScoped
public class GeneService {

    private static GeneRepository geneRepo = new GeneRepository();
    private static InteractionRepository interRepo = new InteractionRepository();
    private static InteractionCacheRepository interCacheRepo = new InteractionCacheRepository();
    private static PhenotypeRepository phenoRepo = new PhenotypeRepository();
    private static PhenotypeCacheRepository phenoCacheRepo = new PhenotypeCacheRepository();
    private AlleleService alleleService = new AlleleService();

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
        JsonResultResponse<Allele> response = alleleService.getAllelesByGene(geneId, pagination);
        if (response == null)
            response = new JsonResultResponse<>();
        long duration = (System.currentTimeMillis() - startTime) / 1000;
        response.setRequestDuration(Long.toString(duration));
        return response;
    }

    public JsonResultResponse<InteractionGeneJoin> getInteractions(String id, Pagination pagination) {
        JsonResultResponse<InteractionGeneJoin> response = new JsonResultResponse<>();
        PaginationResult<InteractionGeneJoin> interactions = interCacheRepo.getInteractionAnnotationList(id, pagination);
        response.addAnnotationSummarySupplementalData(getInteractionSummary(id));
        if (interactions == null)
            return response;
        response.addDistinctFieldValueSupplementalData(interactions.getDistinctFieldValueMap());
        response.setResults(interactions.getResult());
        response.setTotal(interactions.getTotalNumber());
        return response;
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

}
