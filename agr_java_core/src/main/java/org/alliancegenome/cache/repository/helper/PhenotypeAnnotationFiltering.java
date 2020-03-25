package org.alliancegenome.cache.repository.helper;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;

import java.util.Set;
import java.util.stream.Collectors;

public class PhenotypeAnnotationFiltering extends AnnotationFiltering<PhenotypeAnnotation> {


    public FilterFunction<PhenotypeAnnotation, String> termNameFilter =
            (annotation, value) -> FilterFunction.contains(annotation.getPhenotype(), value);

    public FilterFunction<PhenotypeAnnotation, String> geneticEntityFilter =
            (annotation, value) -> {
                if (annotation.getAllele() != null)
                    return FilterFunction.contains(annotation.getAllele().getSymbol(), value);
                else
                    return false;
            };

    public FilterFunction<PhenotypeAnnotation, String> sourceFilter =
            (annotation, value) -> FilterFunction.contains(annotation.getSource().getName(), value);

    public FilterFunction<PhenotypeAnnotation, String> geneticEntityTypeFilter =
            (annotation, value) -> {
                // if a gene
                if (annotation.getAllele() == null) {
                    return FilterFunction.fullMatchMultiValueOR(annotation.getGene().getType(), value);
                } else {
                    return FilterFunction.fullMatchMultiValueOR(annotation.getAllele().getType(), value);
                }
            };

    public FilterFunction<PhenotypeAnnotation, String> referenceFilter =
            (annotation, value) -> {
                Set<Boolean> filteringPassed = annotation.getPublications().stream()
                        .map(publication -> FilterFunction.contains(publication.getPubId(), value))
                        .collect(Collectors.toSet());
                // return true if at least one pub is found
                return filteringPassed.contains(true);
            };

    public PhenotypeAnnotationFiltering() {
        filterFieldMap.put(FieldFilter.PHENOTYPE, termNameFilter);
        filterFieldMap.put(FieldFilter.FREFERENCE, referenceFilter);
        filterFieldMap.put(FieldFilter.GENETIC_ENTITY_TYPE, geneticEntityTypeFilter);
        filterFieldMap.put(FieldFilter.GENETIC_ENTITY, geneticEntityFilter);
    }
/*
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
*/

}

