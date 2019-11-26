package org.alliancegenome.cache.repository;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.manager.BasicCachingManager;
import org.alliancegenome.core.service.*;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.view.BaseFilter;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
public class AlleleCacheRepository {

    // cached value
    private static List<Allele> allAlleles = null;

    public JsonResultResponse<Allele> getAllelesBySpecies(String species, Pagination pagination) {
//todo        List<Allele> allAlleles = taxonAlleleMap.get(species);
        return getAlleleJsonResultResponse(pagination, allAlleles);
    }

    public JsonResultResponse<Allele> getAllelesByGene(String geneID, Pagination pagination) {

        BasicCachingManager<Allele> manager = new BasicCachingManager<>(Allele.class);
        List<Allele> allAlleles = manager.getCache(geneID, CacheAlliance.ALLELE);
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
//todo        taxonAlleleMap.forEach((key, value) -> builder.append(SpeciesType.fromTaxonId(key).getDisplayName() + ": " + value.size() + ", "));
        log.info(builder.toString());

    }

}
