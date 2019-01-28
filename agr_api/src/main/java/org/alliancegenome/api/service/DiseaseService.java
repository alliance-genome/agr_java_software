package org.alliancegenome.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.ogm.model.Result;

import javax.enterprise.context.RequestScoped;
import java.time.LocalDateTime;
import java.util.*;

@RequestScoped
public class DiseaseService {

    private Log log = LogFactory.getLog(getClass());
    private static DiseaseRepository diseaseRepository = new DiseaseRepository();

    public DOTerm getById(String id) {
        return diseaseRepository.getDiseaseTerm(id);
    }

    public JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotationsByDisease(String diseaseID, Pagination pagination) {
        LocalDateTime startDate = LocalDateTime.now();
        List<DiseaseAnnotation> list = getDiseaseAnnotationList(diseaseID, pagination);
        JsonResultResponse<DiseaseAnnotation> response = new JsonResultResponse<>();
        response.calculateRequestDuration(startDate);
        response.setResults(list);
        Long count = diseaseRepository.getTotalDiseaseCount(diseaseID, pagination);
        response.setTotal((int) (long) count);
        return response;
    }

    private List<DiseaseAnnotation> getDiseaseAnnotationList(String diseaseID, Pagination pagination) {
        Result result = diseaseRepository.getDiseaseAssociation(null, diseaseID, pagination, null);
        return getDiseaseAnnotations(result);
    }

    private List<DiseaseAnnotation> getEmpiricalDiseaseAnnotationList(String geneID, Pagination pagination, boolean empiricalDisease) {
        Result result = diseaseRepository.getDiseaseAssociation(geneID, null, pagination, empiricalDisease);
        return getDiseaseAnnotations(result);
    }

    private List<DiseaseAnnotation> getDiseaseAnnotations(Result result) {
        List<DiseaseAnnotation> annotationDocuments = new ArrayList<>();
        result.forEach(objectMap -> {
            DiseaseAnnotation document = new DiseaseAnnotation();
            Gene gene = (Gene) objectMap.get("gene");
            gene.setSpecies((Species) objectMap.get("species"));
            document.setGene(gene);
            DOTerm disease = (DOTerm) objectMap.get("disease");
            document.setDisease(disease);

            DiseaseEntityJoin diseaseEntityJoin = ((List<DiseaseEntityJoin>) objectMap.get("diseaseEntityJoin")).get(0);
            diseaseEntityJoin.setDisease(disease);
            document.setSource(diseaseEntityJoin.getSource());
            document.setAssociationType(diseaseEntityJoin.getJoinType());
            document.setDisease(disease);
            Allele feature = (Allele) objectMap.get("feature");
            document.setDiseaseEntityJoinSet((List<DiseaseEntityJoin>) objectMap.get("diseaseEntityJoin"));
            document.setAssociationType(diseaseEntityJoin.getJoinType());
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

    public JsonResultResponse<DiseaseAnnotation> getEmpiricalDiseaseAnnotations(String id, Pagination pagination, boolean empiricalDisease) throws JsonProcessingException {
        return getDiseaseAnnotations(id, pagination, empiricalDisease);
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

}

