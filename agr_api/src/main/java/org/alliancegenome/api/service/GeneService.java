package org.alliancegenome.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.repository.InteractionRepository;
import org.alliancegenome.neo4j.repository.PhenotypeRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.ogm.model.Result;

import javax.enterprise.context.RequestScoped;
import java.time.LocalDateTime;
import java.util.*;

@RequestScoped
public class GeneService {
    
    private Log log = LogFactory.getLog(getClass());
    private static GeneRepository geneRepo = new GeneRepository();
    private static InteractionRepository interRepo = new InteractionRepository();
    private static PhenotypeRepository phenoRepo = new PhenotypeRepository();
    private static DiseaseRepository diseaseRepository = new DiseaseRepository();

    public Gene getById(String id) {
        Gene gene = geneRepo.getOneGene(id);
        // if not found directly check if it is a secondary id on a different gene
        if (gene == null) {
            return geneRepo.getGeneBySecondary(id);
        }
        return gene;
    }

    public JsonResultResponse<Allele> getAlleles(String id, int limit, int page, String sortBy, String asc) {
        JsonResultResponse<Allele> ret = new JsonResultResponse<>();
        Pagination pagination = new Pagination(page, limit, sortBy, asc);
        //return geneService.getPhenotypeAnnotations(id, pagination);
        
        ret.setResults(geneRepo.getAlleles(id));
        return ret;
    }

    public JsonResultResponse<InteractionGeneJoin> getInteractions(String id) {
        JsonResultResponse<InteractionGeneJoin> ret = new JsonResultResponse<>();
        ret.setResults(interRepo.getInteractions(id));
        return ret;
    }
    
    public JsonResultResponse<PhenotypeAnnotation> getPhenotypeAnnotations(String geneID, Pagination pagination) throws JsonProcessingException {
        LocalDateTime startDate = LocalDateTime.now();
        List<PhenotypeAnnotation> list = getPhenotypeAnnotationList(geneID, pagination);
        JsonResultResponse<PhenotypeAnnotation> response = new JsonResultResponse<>();
        response.calculateRequestDuration(startDate);
        response.setResults(list);
        Long count = phenoRepo.getTotalPhenotypeCount(geneID, pagination);
        response.setTotal((int) (long) count);
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
                List<CrossReference> ref = (List<CrossReference>) objectMap.get("crossReferences");
                allele.setCrossReferences(ref);
                allele.setCrossReferenceType(GeneticEntity.CrossReferenceType.ALLELE);
                allele.setSpecies((Species) objectMap.get("featureSpecies"));
                document.setGeneticEntity(allele);
            } else { // must be a gene for now as we only have features or genes
                Gene gene = (Gene) objectMap.get("gene");
                gene.setCrossReferenceType(GeneticEntity.CrossReferenceType.GENE);
                gene.setSpecies((Species) objectMap.get("geneSpecies"));
                List<CrossReference> ref = (List<CrossReference>) objectMap.get("geneCrossReferences");
                gene.setCrossReferences(ref);
                document.setGeneticEntity(gene);
            }
            document.setPublications((List<Publication>) objectMap.get("publications"));
            annotationDocuments.add(document);
        });

        return annotationDocuments;
    }

    public JsonResultResponse<DiseaseAnnotation> getEmpiricalDiseaseAnnotations(String id, Pagination pagination, boolean empiricalDisease) throws JsonProcessingException {
        return getDiseaseAnnotations(id, pagination, empiricalDisease);
    }
/*

    public JsonResultResponse<DiseaseAnnotation> getEmpiricalDiseaseAnnotations(String id, Pagination pagination, boolean empiricalDisease) throws JsonProcessingException {
        return geneDAO.getEmpiricalDiseaseAnnotations(id, pagination, empiricalDisease);
    }
*/

    

    private List<DiseaseAnnotation> getDiseaseAnnotationList(String diseaseID, Pagination pagination) {
        Result result = diseaseRepository.getDiseaseAssociation(null, diseaseID, pagination, null);
        return getDiseaseAnnotations(diseaseID, result);
    }

    private List<DiseaseAnnotation> getEmpiricalDiseaseAnnotationList(String geneID, Pagination pagination, boolean empiricalDisease) {
        Result result = diseaseRepository.getDiseaseAssociation(geneID, null, pagination, empiricalDisease);
        return getDiseaseAnnotations(geneID, result);
    }

    private List<DiseaseAnnotation> getDiseaseAnnotations(String geneID, Result result) {
        List<DiseaseAnnotation> annotationDocuments = new ArrayList<>();
        result.forEach(objectMap -> {
            DiseaseAnnotation document = new DiseaseAnnotation();
            Gene gene = new Gene();
            gene.setPrimaryKey(geneID);
            document.setGene(gene);
            document.setDisease((DOTerm) objectMap.get("disease"));
            document.setAssociationType(((List<DiseaseEntityJoin>)objectMap.get("diseaseEntityJoin")).get(0).getJoinType());
            Allele feature = (Allele) objectMap.get("feature");
            document.setDiseaseEntityJoinSet((List<DiseaseEntityJoin>) objectMap.get("diseaseEntityJoin"));
            document.setAssociationType(((List<DiseaseEntityJoin>) objectMap.get("diseaseEntityJoin")).get(0).getJoinType());
            document.setEvidenceCodes((List<EvidenceCode>) objectMap.get("evidences"));
            List<Gene> orthoGenes = (List<Gene>) objectMap.get("orthoGenes");
            List<Species> orthoGeneSpecies = (List<Species>) objectMap.get("orthoSpecies");
            if (orthoGenes != null) {
                Set<Gene> orthoGeneSet = new HashSet<>(orthoGenes);
                if (orthoGeneSet.size() > 1)
                    log.warn("Annotation has more than one orthology Gene..." + document.getDisease().getName());
                Gene next = orthoGeneSet.iterator().next();
                next.setSpecies(orthoGeneSpecies.iterator().next());
                document.setOrthologyGene(next);
            }
//            Feature feature = (Feature) objectMap.get("feature");
            if (feature != null) {
                List<CrossReference> ref = (List<CrossReference>) objectMap.get("crossReferences");
                feature.setCrossReferences(ref);
                document.setFeature(feature);
            }
            List<Publication> publicationList = (List<Publication>) objectMap.get("publications");
            publicationList.sort(Comparator.naturalOrder());
            document.setPublications(publicationList);
            annotationDocuments.add(document);
        });

        return annotationDocuments;
    }

    public JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotations(String geneID, Pagination pagination, boolean empiricalDisease) {
        LocalDateTime startDate = LocalDateTime.now();
        List<DiseaseAnnotation> list = getEmpiricalDiseaseAnnotationList(geneID, pagination, empiricalDisease);
        JsonResultResponse<DiseaseAnnotation> response = new JsonResultResponse<>();
        response.calculateRequestDuration(startDate);
        response.setResults(list);
        Long count = diseaseRepository.getTotalDiseaseCount(geneID, pagination, empiricalDisease);
        response.setTotal((int) (long) count);
        return response;
    }
    
    public JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotationsByDisease(String diseaseID, Pagination pagination) {
        LocalDateTime startDate = LocalDateTime.now();
        List<DiseaseAnnotation> list = getDiseaseAnnotationList(diseaseID, pagination);
        JsonResultResponse<DiseaseAnnotation> response = new JsonResultResponse<>();
        response.calculateRequestDuration(startDate);
        response.setResults(list);
//        Long count = diseaseRepository.getTotalDiseaseCount(diseaseID, pagination, empiricalDisease);
//        response.setTotal((int) (long) count);
        response.setTotal(4);
        return response;
    }



}
