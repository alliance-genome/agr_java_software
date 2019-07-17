package org.alliancegenome.neo4j.repository;

import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.cache.AllianceCacheManager;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.core.service.*;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

public class AlleleCacheRepository {

    private Log log = LogFactory.getLog(getClass());
    // cached value
    private static List<Allele> allAlleles = null;
    // Map<gene ID, List<Allele>> grouped by gene ID
    private static Map<String, List<Allele>> geneAlleleMap;
    // Map<taxon ID, List<Allele>> grouped by taxon ID
    private static Map<String, List<Allele>> taxonAlleleMap;

    private static boolean caching;
    private static LocalDateTime start;
    private static LocalDateTime end;

    private AlleleRepository alleleRepo = new AlleleRepository();
    
    
    public JsonResultResponse<Allele> getAllelesBySpecies(String species, Pagination pagination) {
        List<Allele> allAlleles = taxonAlleleMap.get(species);
        return getAlleleJsonResultResponse(pagination, allAlleles);
    }

    public JsonResultResponse<Allele> getAllelesByGene(String geneID, Pagination pagination) {

        List<Allele> allAlleles = AllianceCacheManager.getCacheSpaceWeb(CacheAlliance.ALLELE).get(geneID);
        if (allAlleles == null)
            return null;
        return getAlleleJsonResultResponse(pagination, allAlleles);
    }

    private List<Allele> getSortedAndPaginatedAlleles(List<Allele> alleleList, Pagination pagination) {
        // sorting
        SortingField sortingField = null;
        String sortBy = pagination.getSortBy();
        if (sortBy != null && !sortBy.isEmpty())
            sortingField = SortingField.getSortingField(sortBy.toUpperCase());

        AlleleSorting sorting = new AlleleSorting();
        alleleList.sort(sorting.getComparator(sortingField, pagination.getAsc()));

        // paginating
        return alleleList.stream()
                .skip(pagination.getStart())
                .limit(pagination.getLimit())
                .collect(Collectors.toList());
    }

    private JsonResultResponse<Allele> getAlleleJsonResultResponse(Pagination pagination, List<Allele> allAlleles) {
        JsonResultResponse<Allele> response = new JsonResultResponse<>();

        //filtering
        List<Allele> filteredAlleleList = filterDiseaseAnnotations(allAlleles, pagination.getFieldFilterValueMap());
        response.setResults(getSortedAndPaginatedAlleles(filteredAlleleList, pagination));
        response.setTotal(filteredAlleleList.size());
        return response;
    }

    private List<Allele> filterDiseaseAnnotations(List<Allele> alleleList, BaseFilter fieldFilterValueMap) {
        if (alleleList == null)
            return null;
        if (fieldFilterValueMap == null)
            return alleleList;
        return alleleList.stream()
                .filter(annotation -> containsFilterValue(annotation, fieldFilterValueMap))
                .collect(Collectors.toList());
    }

    private boolean containsFilterValue(Allele allele, BaseFilter fieldFilterValueMap) {
        // remove entries with null values.
        fieldFilterValueMap.values().removeIf(Objects::isNull);

        Set<Boolean> filterResults = fieldFilterValueMap.entrySet().stream()
                .map((entry) -> {
                    FilterFunction<Allele, String> filterFunction = AlleleFiltering.filterFieldMap.get(entry.getKey());
                    if (filterFunction == null)
                        return null;
                    return filterFunction.containsFilterValue(allele, entry.getValue());
                })
                .collect(Collectors.toSet());

        return !filterResults.contains(false);
    }

    private void printTaxonMap() {
        log.info("Taxon / Allele map: ");
        StringBuilder builder = new StringBuilder();
        taxonAlleleMap.forEach((key, value) -> builder.append(SpeciesType.fromTaxonId(key).getDisplayName() + ": " + value.size() + ", "));
        log.info(builder.toString());

    }

    public CacheStatus getCacheStatus() {
        CacheStatus status = new CacheStatus("Allele");
        status.setCaching(caching);
        status.setStart(start);
        status.setEnd(end);
        if (allAlleles != null)
            status.setNumberOfEntities(allAlleles.size());
        return status;
    }
}
