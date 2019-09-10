package org.alliancegenome.cache.repository;

import com.fasterxml.jackson.databind.type.CollectionType;
import lombok.extern.log4j.Log4j2;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.manager.BasicCacheManager;
import org.alliancegenome.cache.manager.DiseaseAllianceCacheManager;
import org.alliancegenome.core.service.*;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.node.ECOTerm;
import org.alliancegenome.neo4j.entity.node.PublicationEvidenceCodeJoin;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toList;

@Log4j2
public class DiseaseCacheRepository {


    // Map<gene ID, List<DiseaseAnnotation>> including annotations to child terms
    private static Map<String, List<DiseaseAnnotation>> diseaseAnnotationExperimentGeneMap = new HashMap<>();
    // Map<gene ID, List<DiseaseAnnotation>> including annotations to child terms
    private static Map<String, List<DiseaseAnnotation>> diseaseAnnotationOrthologGeneMap = new HashMap<>();

    public PaginationResult<DiseaseAnnotation> getDiseaseAnnotationList(String diseaseID, Pagination pagination) {

        DiseaseAllianceCacheManager manager = new DiseaseAllianceCacheManager();
        List<DiseaseAnnotation> fullDiseaseAnnotationList = manager.getDiseaseAnnotations(diseaseID, View.DiseaseCacher.class);
        PaginationResult<DiseaseAnnotation> result = new PaginationResult<>();
        if (fullDiseaseAnnotationList == null) {
            return result;
        }

        //filtering
        List<DiseaseAnnotation> filteredDiseaseAnnotationList = filterDiseaseAnnotations(fullDiseaseAnnotationList, pagination.getFieldFilterValueMap());

        result.setTotalNumber(filteredDiseaseAnnotationList.size());
        result.setResult(getSortedAndPaginatedDiseaseAnnotations(pagination, filteredDiseaseAnnotationList));
        return result;
    }

    public PaginationResult<DiseaseAnnotation> getRibbonDiseaseAnnotations(List<String> geneIDs, String diseaseSlimID, Pagination pagination) {

        if (geneIDs == null)
            return null;
        List<DiseaseAnnotation> fullDiseaseAnnotationList = new ArrayList<>();

        DiseaseAllianceCacheManager manager = new DiseaseAllianceCacheManager();
        // filter by gene
        geneIDs.forEach(geneID -> {
                    List<DiseaseAnnotation> annotations = manager.getDiseaseAnnotations(geneID, View.DiseaseCacher.class);
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

    public List<ECOTerm> getEcoTerm(List<PublicationEvidenceCodeJoin> joins) {
        BasicCacheManager<String> manager = new BasicCacheManager<>();
        List<ECOTerm> list = new ArrayList<>();
        CollectionType javaType = BasicCacheManager.mapper.getTypeFactory()
                .constructCollectionType(List.class, ECOTerm.class);

        joins.forEach(join -> {
            String json = manager.getCache(join.getPrimaryKey(), CacheAlliance.ECO_MAP);
            try {
                list.addAll(BasicCacheManager.mapper.readValue(json, javaType));
            } catch (IOException e) {
                log.error("Error during deserialization ", e);
                throw new RuntimeException(e);
            }
        });
        return list;
    }

    public List<String> getChildren(String id) {
        BasicCacheManager<String> manager = new BasicCacheManager<>();
        List<String> list = new ArrayList<>();
        CollectionType javaType = BasicCacheManager.mapper.getTypeFactory()
                .constructCollectionType(List.class, String.class);

        String json = manager.getCache(id, CacheAlliance.CLOSURE_MAP);
        try {
            list.addAll(BasicCacheManager.mapper.readValue(json, javaType));
        } catch (IOException e) {
            log.error("Error during deserialization ", e);
            throw new RuntimeException(e);
        }
        return list;
    }
}
