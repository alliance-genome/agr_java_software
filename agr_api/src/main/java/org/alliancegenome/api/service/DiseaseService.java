package org.alliancegenome.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.SortingField;
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

import static java.util.stream.Collectors.toList;

@RequestScoped
public class DiseaseService {

    private Log log = LogFactory.getLog(getClass());
    private static DiseaseRepository diseaseRepository = new DiseaseRepository();

    public DiseaseService() {

    }

    public DOTerm getById(String id) {
        return diseaseRepository.getDiseaseTerm(id);
    }

    // cached value
    private static List<DiseaseAnnotation> allDiseaseAnnotations = null;
    // Map<disease ID, List<DiseaseAnnotation>> including annotations to child terms
    private static Map<String, List<DiseaseAnnotation>> diseaseAnnotationMap = new HashMap<>();
    private static boolean caching;

    public JsonResultResponse<DiseaseAnnotation> getDiseaseAnnotationsByDisease(String diseaseID, Pagination pagination) {
        LocalDateTime startDate = LocalDateTime.now();
        List<DiseaseAnnotation> list = getDiseaseAnnotationList(diseaseID, pagination);
        JsonResultResponse<DiseaseAnnotation> response = new JsonResultResponse<>();
        response.calculateRequestDuration(startDate);
        response.setResults(list);
        response.setTotal(getTotalDiseaseAnnotation(diseaseID, pagination));
        return response;
    }

    private int getTotalDiseaseAnnotation(String diseaseID, Pagination pagination) {
        return diseaseAnnotationMap.get(diseaseID).size();
    }

    private List<DiseaseAnnotation> getDiseaseAnnotationList(String diseaseID, Pagination pagination) {
        if (allDiseaseAnnotations == null && !caching) {
            caching = true;
            cacheAllDiseaseAnnotations();
            caching = false;
        }
        if (caching)
            return null;

        List<DiseaseAnnotation> fullDiseaseAnnotationList = diseaseAnnotationMap.get(diseaseID);

        // sorting
        SortingField sortingField = null;
        String sortBy = pagination.getSortBy();
        if (sortBy != null && !sortBy.isEmpty())
            sortingField = SortingField.valueOf(sortBy.toUpperCase());

        DiseaseAnnotationSorting sorting = new DiseaseAnnotationSorting();
        fullDiseaseAnnotationList.sort(sorting.getComparator(sortingField));

        // paginating
        return fullDiseaseAnnotationList.stream()
                .skip(pagination.getStart())
                .limit(pagination.getLimit())
                .collect(Collectors.toList());
    }

    private void cacheAllDiseaseAnnotations() {
        Set<DiseaseEntityJoin> joinList = diseaseRepository.getAllDiseaseEntityJoins();
        if (joinList == null)
            return;
        allDiseaseAnnotations = joinList.stream()
                .map(diseaseEntityJoin -> {
                    DiseaseAnnotation document = new DiseaseAnnotation();
                    document.setGene(diseaseEntityJoin.getGene());
                    document.setFeature(diseaseEntityJoin.getAllele());
                    document.setDisease(diseaseEntityJoin.getDisease());
                    document.setSource(diseaseEntityJoin.getSource());
                    document.setAssociationType(diseaseEntityJoin.getJoinType());
                    document.setSortOrder(diseaseEntityJoin.getSortOrder());
                    List<Publication> publicationList = diseaseEntityJoin.getPublicationEvidenceCodeJoin().stream()
                            .map(PublicationEvidenceCodeJoin::getPublication).sorted(Comparator.naturalOrder()).collect(Collectors.toList());
                    document.setPublications(publicationList.stream().distinct().collect(Collectors.toList()));
                    Set<EvidenceCode> evidences = diseaseEntityJoin.getPublicationEvidenceCodeJoin().stream()
                            .map(PublicationEvidenceCodeJoin::getEvidenceCodes)
                            .flatMap(Collection::stream)
                            .collect(Collectors.toSet());
                    document.setEvidenceCodes(new ArrayList<>(evidences));
                    return document;
                })
                .collect(toList());
        // default sorting
        DiseaseAnnotationSorting sorting = new DiseaseAnnotationSorting();
        allDiseaseAnnotations.sort(sorting.getDefaultComparator());
        log.info("Retrieved " + allDiseaseAnnotations.size() + " annotations");
        long startCreateHistogram = System.currentTimeMillis();
        Map<String, Set<String>> closureMapping = diseaseRepository.getClosureMapping();
        log.info("Number of Disease IDs: " + closureMapping.size());
        final Set<String> allIDs = closureMapping.keySet();

        // loop over all disease IDs (termID)
        // and store the annotations in a map for quick retrieval
        allIDs.forEach(termID -> {
            Set<String> allDiseaseIDs = closureMapping.get(termID);
            List<DiseaseAnnotation> joins = allDiseaseAnnotations.stream()
                    .filter(join -> allDiseaseIDs.contains(join.getDisease().getPrimaryKey()))
                    .collect(Collectors.toList());
            diseaseAnnotationMap.put(termID, joins);
        });
        log.info("Number of Disease IDs in disease Map: " + diseaseAnnotationMap.size());
        log.info("Time to create annotation histogram: " + (System.currentTimeMillis() - startCreateHistogram) / 1000);
    }

    public List<DiseaseAnnotation> getEmpiricalDiseaseAnnotationList(String geneID, Pagination pagination, boolean empiricalDisease) {
        Result result = diseaseRepository.getDiseaseAssociation(geneID, null, pagination, empiricalDisease);
        return getDiseaseAnnotations(result);
    }

    private List<DiseaseAnnotation> getDiseaseAnnotations(Result result) {
        List<DiseaseAnnotation> annotationDocuments = new ArrayList<>();
        if (result == null)
            return null;
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

