package org.alliancegenome.cache.repository.helper;

import java.util.*;
import java.util.stream.Collectors;

import org.alliancegenome.es.model.query.FieldFilter;

public abstract class AnnotationFiltering<T> {

    public Map<FieldFilter, FilterFunction<T, String>> filterFieldMap = new HashMap<>();

    public boolean isValidFiltering(Map<FieldFilter, String> fieldFilterValueMap) {
        if (fieldFilterValueMap == null)
            return true;
        Set<Boolean> result = fieldFilterValueMap.entrySet().stream()
                .map(entry -> filterFieldMap.containsKey(entry.getKey()))
                .collect(Collectors.toSet());
        return !result.contains(false);
    }

    public List<String> getInvalidFieldFilter(Map<FieldFilter, String> fieldFilterValueMap) {
        return fieldFilterValueMap.entrySet().stream()
                .filter(entry -> !filterFieldMap.containsKey(entry.getKey()))
                .map(entry -> entry.getKey().getFullName())
                .collect(Collectors.toList());
    }

}

