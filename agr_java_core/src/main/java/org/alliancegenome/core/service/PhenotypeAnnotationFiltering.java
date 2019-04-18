package org.alliancegenome.core.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;

public class PhenotypeAnnotationFiltering {


    public static FilterFunction<PhenotypeAnnotation, String> termNameFilter =
            (annotation, value) -> FilterFunction.contains(annotation.getPhenotype(), value);

    public static FilterFunction<PhenotypeAnnotation, String> geneticEntityFilter =
            (annotation, value) -> {
                return FilterFunction.contains(annotation.getGeneticEntity().getSymbol(), value);
            };

    public static FilterFunction<PhenotypeAnnotation, String> sourceFilter =
            (annotation, value) -> FilterFunction.contains(annotation.getSource().getName(), value);

    public static FilterFunction<PhenotypeAnnotation, String> geneticEntityTypeFilter =
            (annotation, value) -> FilterFunction.fullMatchMultiValueOR(annotation.getGeneticEntity().getType(), value);

    public static FilterFunction<PhenotypeAnnotation, String> referenceFilter =
            (annotation, value) -> {
                Set<Boolean> filteringPassed = annotation.getPublications().stream()
                        .map(publication -> FilterFunction.contains(publication.getPubId(), value))
                        .collect(Collectors.toSet());
                // return true if at least one pub is found
                return filteringPassed.contains(true);
            };

    public static Map<FieldFilter, FilterFunction<PhenotypeAnnotation, String>> filterFieldMap = new HashMap<>();

    static {
        filterFieldMap.put(FieldFilter.PHENOTYPE, termNameFilter);
        filterFieldMap.put(FieldFilter.REFERENCE, referenceFilter);
        filterFieldMap.put(FieldFilter.GENETIC_ENTITY_TYPE, geneticEntityTypeFilter);
        filterFieldMap.put(FieldFilter.GENETIC_ENTITY, geneticEntityFilter);
    }

    public static boolean isValidFiltering(Map<FieldFilter, String> fieldFilterValueMap) {
        if (fieldFilterValueMap == null)
            return true;
        Set<Boolean> result = fieldFilterValueMap.entrySet().stream()
                .map(entry -> filterFieldMap.containsKey(entry.getKey()))
                .collect(Collectors.toSet());
        return !result.contains(false);
    }

    public static List<String> getInvalidFieldFilter(Map<FieldFilter, String> fieldFilterValueMap) {
        return fieldFilterValueMap.entrySet().stream()
                .filter(entry -> !filterFieldMap.containsKey(entry.getKey()))
                .map(entry -> entry.getKey().getFullName())
                .collect(Collectors.toList());
    }

}

