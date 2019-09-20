package org.alliancegenome.api.service;

import org.alliancegenome.core.service.AnnotationFiltering;
import org.alliancegenome.core.service.FilterFunction;
import org.alliancegenome.core.service.SortingField;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.Sorting;
import org.alliancegenome.neo4j.view.BaseFilter;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class FilterService<T> {

    private AnnotationFiltering<T> filtering;

    public FilterService(AnnotationFiltering<T> filtering) {
        super();
        this.filtering = filtering;
    }

    public List<T> filterAnnotations(List<T> annotationList, BaseFilter fieldFilterValueMap) {
        if (annotationList == null)
            return null;
        if (fieldFilterValueMap == null)
            return annotationList;
        return annotationList.stream()
                .filter(annotation -> containsFilterValue(annotation, fieldFilterValueMap))
                .collect(Collectors.toList());
    }

    public boolean containsFilterValue(T annotation, BaseFilter fieldFilterValueMap) {
        // remove entries with null values.
        fieldFilterValueMap.values().removeIf(Objects::isNull);

        Set<Boolean> filterResults = fieldFilterValueMap.entrySet().stream()
                .map((entry) -> {
                    FilterFunction<T, String> filterFunction = filtering.filterFieldMap.get(entry.getKey());
                    if (filterFunction == null)
                        return null;
                    return filterFunction.containsFilterValue(annotation, entry.getValue());
                })
                .collect(Collectors.toSet());

        return !filterResults.contains(false);
    }

    public List<T> getSortedAndPaginatedAnnotations(Pagination pagination, List<T> fullDiseaseAnnotationList, Sorting<T> sorting) {
        // sorting
        SortingField sortingField = null;
        String sortBy = pagination.getSortBy();
        if (sortBy != null && !sortBy.isEmpty())
            sortingField = SortingField.getSortingField(sortBy.toUpperCase());

        fullDiseaseAnnotationList.sort(sorting.getComparator(sortingField, pagination.getAsc()));

        // paginating
        return fullDiseaseAnnotationList.stream()
                .skip(pagination.getStart())
                .limit(pagination.getLimit())
                .collect(Collectors.toList());
    }
}

