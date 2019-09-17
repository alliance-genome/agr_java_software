package org.alliancegenome.core.service;

import org.alliancegenome.es.model.query.FieldFilter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class AnnotationFiltering<T> {

    public Map<FieldFilter, FilterFunction<T, String>> filterFieldMap = new HashMap<>();

/*

    static {
        filterFieldMap.put(FieldFilter.MOLECULE_TYPE, interactorMoleculeTypeFilter);
        filterFieldMap.put(FieldFilter.INTERACTOR_GENE_SYMBOL, interactorGeneSymbolFilter);
        filterFieldMap.put(FieldFilter.FREFERENCE, referenceFilter);
        filterFieldMap.put(FieldFilter.SOURCE, sourceFilter);
    }
*/

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

