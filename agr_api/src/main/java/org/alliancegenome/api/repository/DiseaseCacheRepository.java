package org.alliancegenome.api.repository;

import org.alliancegenome.api.service.DiseaseRibbonService;
import org.alliancegenome.core.service.*;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.alliancegenome.neo4j.repository.GeneCacheRepository;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

public class DiseaseCacheRepository {

    private Log log = LogFactory.getLog(getClass());
    private static DiseaseRepository diseaseRepository = new DiseaseRepository();
    private GeneCacheRepository geneCacheRepository = new GeneCacheRepository();


    // cached value
    private static List<DiseaseAnnotation> allDiseaseAnnotations = null;
    // Map<disease ID, List<DiseaseAnnotation>> including annotations to child terms
    private static Map<String, List<DiseaseAnnotation>> diseaseAnnotationMap = new HashMap<>();
    // Map<disease ID, List<DiseaseAnnotation>> including annotations to child terms
    private static Map<String, List<DiseaseAnnotation>> diseaseAnnotationSummaryMap = new HashMap<>();
    // Map<gene ID, List<DiseaseAnnotation>> including annotations to child terms
    private static Map<String, List<DiseaseAnnotation>> diseaseAnnotationExperimentGeneMap = new HashMap<>();
    // Map<gene ID, List<DiseaseAnnotation>> including annotations to child terms
    private static Map<String, List<DiseaseAnnotation>> diseaseAnnotationOrthologGeneMap = new HashMap<>();
    private static boolean caching;

    public PaginationResult<DiseaseAnnotation> getDiseaseAnnotationList(String diseaseID, Pagination pagination) {
        checkCache();
        if (caching)
            return null;

        List<DiseaseAnnotation> fullDiseaseAnnotationList = diseaseAnnotationSummaryMap.get(diseaseID);

        //filtering
        List<DiseaseAnnotation> filteredDiseaseAnnotationList = filterDiseaseAnnotations(fullDiseaseAnnotationList, pagination.getFieldFilterValueMap());

        PaginationResult<DiseaseAnnotation> result = new PaginationResult<>();
        result.setTotalNumber(filteredDiseaseAnnotationList.size());
        result.setResult(getSortedAndPaginatedDiseaseAnnotations(pagination, filteredDiseaseAnnotationList));
        return result;
    }

    public PaginationResult<DiseaseAnnotation> getRibbonDiseaseAnnotations(List<String> geneIDs, String diseaseSlimID, Pagination pagination) {
        checkCache();
        if (caching)
            return null;

        if (geneIDs == null)
            return null;
        List<DiseaseAnnotation> fullDiseaseAnnotationList = new ArrayList<>();
        // filter by gene
        geneIDs.forEach(geneID -> {
                    List<DiseaseAnnotation> annotations = diseaseAnnotationExperimentGeneMap.get(geneID);
                    if (annotations != null)
                        fullDiseaseAnnotationList.addAll(annotations);
                    else
                        log.info("no disease annotation found for gene: " + geneID);
                }
        );
        // filter by slim ID
        List<DiseaseAnnotation> slimDiseaseAnnotationList = fullDiseaseAnnotationList;
        if (StringUtils.isNotEmpty(diseaseSlimID)) {
            slimDiseaseAnnotationList = fullDiseaseAnnotationList.stream()
                    .filter(diseaseAnnotation -> diseaseAnnotation.getParentIDs().contains(diseaseSlimID))
                    .collect(toList());
        }

        //filtering
        List<DiseaseAnnotation> filteredDiseaseAnnotationList = filterDiseaseAnnotations(slimDiseaseAnnotationList, pagination.getFieldFilterValueMap());

        PaginationResult<DiseaseAnnotation> result = new PaginationResult<>();
        result.setTotalNumber(filteredDiseaseAnnotationList.size());
        result.setResult(getSortedAndPaginatedDiseaseAnnotations(pagination, filteredDiseaseAnnotationList));
        return result;
    }

    public PaginationResult<DiseaseAnnotation> getDiseaseAnnotationList(String geneID, Pagination pagination, boolean empiricalDisease) {
        checkCache();
        if (caching)
            return null;

        List<DiseaseAnnotation> diseaseAnnotationList;
        if (empiricalDisease)
            diseaseAnnotationList = diseaseAnnotationExperimentGeneMap.get(geneID);
        else
            diseaseAnnotationList = diseaseAnnotationOrthologGeneMap.get(geneID);
        if (diseaseAnnotationList == null)
            return null;

        //filtering
        List<DiseaseAnnotation> filteredDiseaseAnnotationList = filterDiseaseAnnotations(diseaseAnnotationList, pagination.getFieldFilterValueMap());
        PaginationResult<DiseaseAnnotation> result = new PaginationResult<>();
        result.setTotalNumber(filteredDiseaseAnnotationList.size());

        // sorting
        result.setResult(getSortedAndPaginatedDiseaseAnnotations(pagination, filteredDiseaseAnnotationList));
        return result;
    }

    private List<DiseaseAnnotation> getSortedAndPaginatedDiseaseAnnotations(Pagination pagination, List<DiseaseAnnotation> fullDiseaseAnnotationList) {
        // sorting
        SortingField sortingField = null;
        String sortBy = pagination.getSortBy();
        if (sortBy != null && !sortBy.isEmpty())
            sortingField = SortingField.getSortingField(sortBy.toUpperCase());

        DiseaseAnnotationSorting sorting = new DiseaseAnnotationSorting();
        fullDiseaseAnnotationList.sort(sorting.getComparator(sortingField, pagination.getAsc()));

        // paginating
        return fullDiseaseAnnotationList.stream()
                .skip(pagination.getStart())
                .limit(pagination.getLimit())
                .collect(Collectors.toList());
    }


    private List<DiseaseAnnotation> filterDiseaseAnnotations(List<DiseaseAnnotation> diseaseAnnotationList, BaseFilter fieldFilterValueMap) {
        if (diseaseAnnotationList == null)
            return null;
        if (fieldFilterValueMap == null)
            return diseaseAnnotationList;
        return diseaseAnnotationList.stream()
                .filter(annotation -> containsFilterValue(annotation, fieldFilterValueMap))
                .collect(Collectors.toList());
    }

    private boolean containsFilterValue(DiseaseAnnotation annotation, BaseFilter fieldFilterValueMap) {
        // remove entries with null values.
        fieldFilterValueMap.values().removeIf(Objects::isNull);

        Set<Boolean> filterResults = fieldFilterValueMap.entrySet().stream()
                .map((entry) -> {
                    FilterFunction<DiseaseAnnotation, String> filterFunction = DiseaseAnnotationFiltering.filterFieldMap.get(entry.getKey());
                    if (filterFunction == null)
                        return null;
                    return filterFunction.containsFilterValue(annotation, entry.getValue());
                })
                .collect(Collectors.toSet());

        return !filterResults.contains(false);
    }

    public boolean getCacheStatus() {
        return caching;
    }

    private void checkCache() {
        if (allDiseaseAnnotations == null && !caching) {
            caching = true;
            cacheAllDiseaseAnnotations();
            caching = false;
        }
    }

    private void cacheAllDiseaseAnnotations() {
        Set<DiseaseEntityJoin> joinList = diseaseRepository.getAllDiseaseEntityJoins();
        if (joinList == null)
            return;

        // grouping orthologous records
        List<DiseaseAnnotation> summaryList = new ArrayList<>();

        // replace Gene references with the cached Gene references to keep the memory imprint low.
        DiseaseRibbonService diseaseRibbonService = new DiseaseRibbonService();

        allDiseaseAnnotations = joinList.stream()
                .map(diseaseEntityJoin -> {
                    DiseaseAnnotation document = new DiseaseAnnotation();
                    document.setGene(diseaseEntityJoin.getGene());
                    document.setFeature(diseaseEntityJoin.getAllele());
                    document.setDisease(diseaseEntityJoin.getDisease());
                    document.setSource(diseaseEntityJoin.getSource());
                    document.setAssociationType(diseaseEntityJoin.getJoinType());
                    document.setSortOrder(diseaseEntityJoin.getSortOrder());
                    Gene orthologyGene = diseaseEntityJoin.getOrthologyGene();
                    if (orthologyGene != null) {
                        document.setOrthologyGene(orthologyGene);
                        document.addOrthologousGene(orthologyGene);
// for memory savings reason use cached gene objects.
//                        document.setOrthologyGene(geneCacheRepository.getGene(orthologyGene.getPrimaryKey()));
                    }
                    List<Publication> publicationList = diseaseEntityJoin.getPublicationEvidenceCodeJoin().stream()
                            .map(PublicationEvidenceCodeJoin::getPublication).sorted(Comparator.naturalOrder()).collect(Collectors.toList());
                    document.setPublications(publicationList.stream().distinct().collect(Collectors.toList()));
                    // work around as I cannot figure out how to include the ECOTerm in the overall query without slowing down the performance.
/*
                    Set<ECOTerm> evidences = diseaseEntityJoin.getPublicationEvidenceCodeJoin().stream()
                            .map(PublicationEvidenceCodeJoin::getEcoCode)
                            .flatMap(Collection::stream)
                            .collect(Collectors.toSet());
*/
                    List<ECOTerm> evidences = diseaseRepository.getEcoTerm(diseaseEntityJoin.getPublicationEvidenceCodeJoin());
                    document.setEcoCodes(evidences);
                    Set<String> slimId = diseaseRibbonService.getSlimId(diseaseEntityJoin.getDisease().getPrimaryKey());
                    document.setParentIDs(slimId);
                    return document;
                })
                .collect(toList());

        // default sorting
        DiseaseAnnotationSorting sorting = new DiseaseAnnotationSorting();
        allDiseaseAnnotations.sort(sorting.getComparator(SortingField.DEFAULT, Boolean.TRUE));

        int currentHashCode = 0;
        for (DiseaseAnnotation document : allDiseaseAnnotations) {
            int hash = document.hashCode();
            if (currentHashCode == hash) {
                summaryList.get(summaryList.size() - 1).addOrthologousGene(document.getOrthologyGene());
            } else {
                summaryList.add(document);
            }
            currentHashCode = hash;
        }


        log.info("Retrieved " + String.format("%,d", allDiseaseAnnotations.size()) + " annotations");
        long startCreateHistogram = System.currentTimeMillis();
        Map<String, Set<String>> closureMapping = diseaseRepository.getClosureMapping();
        log.info("Number of Disease IDs: " + closureMapping.size());
        final Set<String> allIDs = closureMapping.keySet();

        long start = System.currentTimeMillis();
        // loop over all disease IDs (termID)
        // and store the annotations in a map for quick retrieval
        allIDs.forEach(termID -> {
            Set<String> allDiseaseIDs = closureMapping.get(termID);
            List<DiseaseAnnotation> joins = allDiseaseAnnotations.stream()
                    .filter(join -> allDiseaseIDs.contains(join.getDisease().getPrimaryKey()))
                    .collect(Collectors.toList());
            diseaseAnnotationMap.put(termID, joins);

            List<DiseaseAnnotation> summaryJoins = summaryList.stream()
                    .filter(join -> allDiseaseIDs.contains(join.getDisease().getPrimaryKey()))
                    .collect(Collectors.toList());
            diseaseAnnotationSummaryMap.put(termID, summaryJoins);
        });

        System.out.println("Time populating diseaseAnnotationMap:  " + ((System.currentTimeMillis() - start) / 1000) + " s");

        // group by gene IDs
        diseaseAnnotationExperimentGeneMap = allDiseaseAnnotations.stream()
                .filter(annotation -> annotation.getSortOrder() < 10)
                .collect(groupingBy(o -> o.getGene().getPrimaryKey(), Collectors.toList()));

        diseaseAnnotationOrthologGeneMap = allDiseaseAnnotations.stream()
                .filter(annotation -> annotation.getSortOrder() == 10)
                .collect(groupingBy(o -> o.getGene().getPrimaryKey(), Collectors.toList()));

        log.info("Number of Disease IDs in disease Map: " + diseaseAnnotationMap.size());
        log.info("Time to create annotation histogram: " + (System.currentTimeMillis() - startCreateHistogram) / 1000);
    }


}
