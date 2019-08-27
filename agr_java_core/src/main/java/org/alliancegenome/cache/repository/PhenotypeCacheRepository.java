package org.alliancegenome.cache.repository;

import static java.util.stream.Collectors.toList;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.alliancegenome.cache.manager.PhenotypeCacheManager;
import org.alliancegenome.core.service.FilterFunction;
import org.alliancegenome.core.service.PaginationResult;
import org.alliancegenome.core.service.PhenotypeAnnotationFiltering;
import org.alliancegenome.core.service.PhenotypeAnnotationSorting;
import org.alliancegenome.core.service.SortingField;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.view.BaseFilter;
import org.alliancegenome.neo4j.view.View;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class PhenotypeCacheRepository {

    public PaginationResult<PhenotypeAnnotation> getPhenotypeAnnotationList(String geneID, Pagination pagination) {

        PhenotypeCacheManager manager = new PhenotypeCacheManager();
        List<PhenotypeAnnotation> fullPhenotypeAnnotationList = manager.getPhenotypeAnnotations(geneID, View.PhenotypeAPI.class);

        //filtering
        List<PhenotypeAnnotation> filteredPhenotypeAnnotationList = filterDiseaseAnnotations(fullPhenotypeAnnotationList, pagination.getFieldFilterValueMap());

        PaginationResult<PhenotypeAnnotation> result = new PaginationResult<>();
        if (filteredPhenotypeAnnotationList != null) {
            result.setTotalNumber(filteredPhenotypeAnnotationList.size());
            result.setResult(getSortedAndPaginatedDiseaseAnnotations(pagination, filteredPhenotypeAnnotationList));
        }
        return result;
    }

    private List<PhenotypeAnnotation> getSortedAndPaginatedDiseaseAnnotations(Pagination pagination, List<PhenotypeAnnotation> fullDiseaseAnnotationList) {
        // sorting
        SortingField sortingField = null;
        String sortBy = pagination.getSortBy();
        if (sortBy != null && !sortBy.isEmpty())
            sortingField = SortingField.getSortingField(sortBy.toUpperCase());

        PhenotypeAnnotationSorting sorting = new PhenotypeAnnotationSorting();
        fullDiseaseAnnotationList.sort(sorting.getComparator(sortingField, pagination.getAsc()));

        // paginating
        return fullDiseaseAnnotationList.stream()
                .skip(pagination.getStart())
                .limit(pagination.getLimit())
                .collect(toList());
    }


    private List<PhenotypeAnnotation> filterDiseaseAnnotations(List<PhenotypeAnnotation> phenotypeAnnotationList, BaseFilter fieldFilterValueMap) {
        if (phenotypeAnnotationList == null)
            return null;
        if (fieldFilterValueMap == null)
            return phenotypeAnnotationList;
        return phenotypeAnnotationList.stream()
                .filter(annotation -> containsFilterValue(annotation, fieldFilterValueMap))
                .collect(Collectors.toList());
    }

    private boolean containsFilterValue(PhenotypeAnnotation annotation, BaseFilter fieldFilterValueMap) {
        // remove entries with null values.
        fieldFilterValueMap.values().removeIf(Objects::isNull);

        Set<Boolean> filterResults = fieldFilterValueMap.entrySet().stream()
                .map((entry) -> {
                    FilterFunction<PhenotypeAnnotation, String> filterFunction = PhenotypeAnnotationFiltering.filterFieldMap.get(entry.getKey());
                    if (filterFunction == null)
                        return null;
                    return filterFunction.containsFilterValue(annotation, entry.getValue());
                })
                .collect(Collectors.toSet());

        return !filterResults.contains(false);
    }

}
