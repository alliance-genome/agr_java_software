package org.alliancegenome.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.PaginationResult;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.EntitySummary;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.repository.*;
import org.neo4j.ogm.model.Result;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

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
        Long duration = (System.currentTimeMillis() - startTime) / 1000;
        response.setRequestDuration(duration.toString());
        return response;
    }

    public JsonResultResponse<InteractionGeneJoin> getInteractions(String id, Pagination pagination) {
        JsonResultResponse<InteractionGeneJoin> ret = new JsonResultResponse<>();
        PaginationResult<InteractionGeneJoin> interactions = interCacheRepo.getInteractionAnnotationList(id, pagination);
        ret.setResults(interactions.getResult());
        ret.setTotal(interactions.getTotalNumber());
        ret.addAnnotationSummarySupplementalData(getInteractionSummary(id));
        return ret;
    }

    public JsonResultResponse<PhenotypeAnnotation> getPhenotypeAnnotations(String geneID, Pagination pagination) throws JsonProcessingException {
        LocalDateTime startDate = LocalDateTime.now();
        PaginationResult<PhenotypeAnnotation> list = phenoCacheRepo.getPhenotypeAnnotationList(geneID, pagination);
        JsonResultResponse<PhenotypeAnnotation> response = new JsonResultResponse<>();
        response.calculateRequestDuration(startDate);
        response.setResults(list.getResult());
        response.setTotal(list.getTotalNumber());
        return response;
    }

    private List<PhenotypeAnnotation> getPhenotypeAnnotationList(String geneID, Pagination pagination) {

        Result result = phenoRepo.getPhenotype(geneID, pagination);
        List<PhenotypeAnnotation> annotationDocuments = new ArrayList<>();
        result.forEach(objectMap -> {
            PhenotypeAnnotation document = new PhenotypeAnnotation();
            document.setPhenotype((String) objectMap.get("phenotype"));
            Allele allele = (Allele) objectMap.get("feature");
            if (allele != null) {
                List<CrossReference> ref = new ArrayList<>();
                ref.add((CrossReference) objectMap.get("pimaryReference"));
                allele.setCrossReferences(ref);
                allele.setCrossReferenceType(GeneticEntity.CrossReferenceType.ALLELE);
                allele.setSpecies((Species) objectMap.get("featureSpecies"));
                document.setGeneticEntity(allele);
            } else { // must be a gene for now as we only have features or genes
                Gene gene = (Gene) objectMap.get("gene");
                gene.setCrossReferenceType(GeneticEntity.CrossReferenceType.GENE);
                gene.setSpecies((Species) objectMap.get("geneSpecies"));
                document.setGeneticEntity(gene);
            }
            List<Publication> publications = (List<Publication>) objectMap.get("publications");
            document.setPublications(publications.stream().distinct().collect(Collectors.toList()));
            annotationDocuments.add(document);
        });

        return annotationDocuments;
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

/*

    public JsonResultResponse<DiseaseAnnotation> getEmpiricalDiseaseAnnotations(String id, Pagination pagination, boolean empiricalDisease) throws JsonProcessingException {
        return geneDAO.getEmpiricalDiseaseAnnotations(id, pagination, empiricalDisease);
    }
*/


}
