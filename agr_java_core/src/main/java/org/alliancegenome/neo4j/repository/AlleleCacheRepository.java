package org.alliancegenome.neo4j.repository;

import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.core.service.*;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.GeneticEntity;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

public class AlleleCacheRepository {

    public JsonResultResponse<Allele> getAllelesBySpecies(String species, Pagination pagination) {
        checkCache();
        if (caching)
            return null;

        List<Allele> allAlleles = taxonAlleleMap.get(species);
        return getAlleleJsonResultResponse(pagination, allAlleles);
    }

    public JsonResultResponse<Allele> getAllelesByGene(String geneID, Pagination pagination) {
        checkCache();
        if (caching)
            return null;

        List<Allele> allAlleles = geneAlleleMap.get(geneID);
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

    private void checkCache() {
        if (allAlleles == null && !caching) {
            caching = true;
            cacheAllAlleles();
            caching = false;
        }
    }

    private void cacheAllAlleles() {
        start = LocalDateTime.now();
        long startTime = System.currentTimeMillis();
        Set<Allele> allAlleleSet = alleleRepo.getAllAlleles();
        if (allAlleleSet == null)
            return;

        allAlleles = new ArrayList<>(allAlleleSet);
        allAlleles.sort(Comparator.comparing(GeneticEntity::getSymbol));
        geneAlleleMap = allAlleles.stream()
                .collect(groupingBy(allele -> allele.getGene().getPrimaryKey()));


        taxonAlleleMap = allAlleles.stream()
                .collect(groupingBy(allele -> allele.getSpecies().getPrimaryKey()));

        log.info("Number of all Alleles: " + allAlleles.size());
        log.info("Number of all Genes with Alleles: " + geneAlleleMap.size());
        printTaxonMap();
        log.info("Time to create cache: " + (System.currentTimeMillis() - startTime) / 1000);
        end = LocalDateTime.now();
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
