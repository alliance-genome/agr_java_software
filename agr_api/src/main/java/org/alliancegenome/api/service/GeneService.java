package org.alliancegenome.api.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.enterprise.context.RequestScoped;

import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.PaginationResult;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.EntitySummary;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.CrossReference;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.GeneticEntity;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.entity.node.Publication;
import org.alliancegenome.neo4j.entity.node.Species;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.repository.InteractionRepository;
import org.alliancegenome.neo4j.repository.PhenotypeCacheRepository;
import org.alliancegenome.neo4j.repository.PhenotypeRepository;
import org.neo4j.ogm.model.Result;

import com.fasterxml.jackson.core.JsonProcessingException;

@RequestScoped
public class GeneService {

    private static GeneRepository geneRepo = new GeneRepository();
    private static InteractionRepository interRepo = new InteractionRepository();
    private static PhenotypeRepository phenoRepo = new PhenotypeRepository();
    private static PhenotypeCacheRepository phenoCacheRepo = new PhenotypeCacheRepository();

    public Gene getById(String id) {
        Gene gene = geneRepo.getOneGene(id);
        // if not found directly check if it is a secondary id on a different gene
        if (gene == null) {
            return geneRepo.getOneGeneBySecondaryId(id);
        }
        return gene;
    }

    // ToDo: Needs pagination logic
    public JsonResultResponse<Allele> getAlleles(String id, int limit, int page, String sortBy, String asc) {
        JsonResultResponse<Allele> ret = new JsonResultResponse<>();
        List<Allele> alleles = geneRepo.getAlleles(id);
        ret.setResults(alleles);
        ret.setTotal(alleles.size());
        return ret;
    }

    public JsonResultResponse<InteractionGeneJoin> getInteractions(String id) {
        JsonResultResponse<InteractionGeneJoin> ret = new JsonResultResponse<>();
        ret.setResults(interRepo.getInteractions(id));
        ret.addSupplementalData("interactionSummary", getInteractionSummary(id));
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
