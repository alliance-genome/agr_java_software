package org.alliancegenome.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.DiseaseSummary;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.neo4j.ogm.model.Result;

import javax.enterprise.context.RequestScoped;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

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
        Result result = diseaseRepository.getDiseaseAssociations(diseaseID, pagination);
        return getDiseaseAnnotations(result);
    }

    public List<DiseaseAnnotation> getEmpiricalDiseaseAnnotationList(String geneID, Pagination pagination, boolean empiricalDisease) {
        Result result = diseaseRepository.getDiseaseAssociation(geneID, null, pagination, empiricalDisease);
        return getDiseaseAnnotations(result);
    }

    private List<DiseaseAnnotation> getDiseaseAnnotations(Result result) {
        List<DiseaseAnnotation> annotationDocuments = new ArrayList<>();
        result.forEach(objectMap -> {
            DiseaseAnnotation document = new DiseaseAnnotation();
            Gene gene = (Gene) objectMap.get("gene");
            gene.setSpecies((Species) objectMap.get("species"));
//            gene.setUrl(((CrossReference) objectMap.get("geneCrossReference")).getCrossRefCompleteUrl());
            document.setGene(gene);
            DOTerm disease = (DOTerm) objectMap.get("disease");
            document.setDisease(disease);


            DiseaseEntityJoin join = (DiseaseEntityJoin) objectMap.get("diseaseEntityJoin");
            DiseaseEntityJoin diseaseEntityJoin = diseaseRepository.getDiseaseEntityJoinByID(join.getPrimaryKey());
            diseaseEntityJoin.setDisease(disease);
            document.setSource(diseaseEntityJoin.getSource());
            document.setAssociationType(diseaseEntityJoin.getJoinType());
///            document.setDiseaseEntityJoinSet(diseaseEntityJoin1);
            document.setDisease(disease);
            Set<EvidenceCode> evidences = diseaseEntityJoin.getPublicationEvidenceCodeJoin().stream()
                    .map(PublicationEvidenceCodeJoin::getEvidenceCodes)
                    .flatMap(Collection::stream)
                    .collect(Collectors.toSet());
            document.setEvidenceCodes(new ArrayList<>(evidences));
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
            if (diseaseEntityJoin.getAllele() != null) {
                document.setFeature(diseaseEntityJoin.getAllele());
            }
            List<Publication> publicationList = diseaseEntityJoin.getPublicationEvidenceCodeJoin().stream()
                    .map(PublicationEvidenceCodeJoin::getPublication)
                    .collect(Collectors.toList());
            publicationList.sort(Comparator.naturalOrder());
            document.setPublications(publicationList.stream().distinct().collect(Collectors.toList()));
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

    public DiseaseSummary getDiseaseSummary(String id, DiseaseSummary.Type type) {
        return diseaseRepository.getDiseaseSummary(id, type);
    }
}

