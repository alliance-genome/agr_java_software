package org.alliancegenome.api;

import static java.util.stream.Collectors.toList;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.alliancegenome.cache.DiseaseAllianceCacheManager;
import org.alliancegenome.core.service.DiseaseAnnotationFiltering;
import org.alliancegenome.core.service.DiseaseAnnotationSorting;
import org.alliancegenome.core.service.FilterFunction;
import org.alliancegenome.core.service.PaginationResult;
import org.alliancegenome.core.service.SortingField;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.lang3.StringUtils;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class DiseaseCacheRepository {

    private static DiseaseRepository diseaseRepository = new DiseaseRepository();

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

    //private static LocalDateTime start;
    //private static LocalDateTime end;

    public PaginationResult<DiseaseAnnotation> getDiseaseAnnotationList(String diseaseID, Pagination pagination) {

        DiseaseAllianceCacheManager manager = new DiseaseAllianceCacheManager();
        List<DiseaseAnnotation> fullDiseaseAnnotationList = manager.getDiseaseAnnotations(diseaseID, View.DiseaseAnnotationSummary.class);
        //filtering
        List<DiseaseAnnotation> filteredDiseaseAnnotationList = filterDiseaseAnnotations(fullDiseaseAnnotationList, pagination.getFieldFilterValueMap());

        PaginationResult<DiseaseAnnotation> result = new PaginationResult<>();
        result.setTotalNumber(filteredDiseaseAnnotationList.size());
        result.setResult(getSortedAndPaginatedDiseaseAnnotations(pagination, filteredDiseaseAnnotationList));
        return result;
    }

    public PaginationResult<DiseaseAnnotation> getRibbonDiseaseAnnotations(List<String> geneIDs, String diseaseSlimID, Pagination pagination) {

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

}
