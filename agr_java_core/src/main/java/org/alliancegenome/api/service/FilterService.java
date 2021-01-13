package org.alliancegenome.api.service;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.alliancegenome.cache.repository.helper.*;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.Sorting;
import org.alliancegenome.neo4j.view.BaseFilter;

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
        if (sorting != null) {
            SortingField sortingField = null;
            String sortBy = pagination.getSortBy();
            if (sortBy != null && !sortBy.isEmpty())
                sortingField = SortingField.getSortingField(sortBy.toUpperCase());

            fullDiseaseAnnotationList.sort(sorting.getComparator(sortingField, pagination.getAsc()));
        }

        // paginating
        return fullDiseaseAnnotationList.stream()
                .skip(pagination.getStart())
                .limit(pagination.getLimit())
                .collect(Collectors.toList());
    }

    public List<T> getPaginatedAnnotations(Pagination pagination, List<T> list) {
        // paginating
        return list.stream()
                .skip(pagination.getStart())
                .limit(pagination.getLimit())
                .collect(Collectors.toList());
    }

    public Map<String, List<String>> getDistinctFieldValues(List<T> list, Map<Column, Function<T, Set<String>>> fieldValueMap, ColumnFieldMapping mapping) {
        Map<String, List<String>> map = new HashMap<>();
        fieldValueMap.forEach((column, function) -> {
            Set<String> distinctValues = new HashSet<>();
            list.forEach(entity -> distinctValues.addAll(function.apply(entity)));
            ArrayList<String> valueList = new ArrayList<>(distinctValues);
            valueList.sort(Comparator.naturalOrder());
            map.put(mapping.getFieldFilterName(column), valueList);
        });
        return map;
    }
}

