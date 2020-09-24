package org.alliancegenome.cache.repository.helper;

import java.util.Set;
import java.util.stream.Collectors;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;

public class PhenotypeAnnotationFiltering extends AnnotationFiltering<PhenotypeAnnotation> {


    private FilterFunction<PhenotypeAnnotation, String> termNameFilter =
            (annotation, value) -> FilterFunction.contains(annotation.getPhenotype(), value);

    private FilterFunction<PhenotypeAnnotation, String> geneticEntityFilter =
            (annotation, value) -> {
                if (annotation.getAllele() != null)
                    return FilterFunction.contains(annotation.getAllele().getSymbol(), value);
                else
                    return false;
            };

    private FilterFunction<PhenotypeAnnotation, String> geneticEntityTypeFilter =
            (annotation, value) -> {
                // if a gene
                if (annotation.getAllele() == null) {
                    return FilterFunction.fullMatchMultiValueOR(annotation.getGene().getType(), value);
                } else {
                    return FilterFunction.fullMatchMultiValueOR(annotation.getAllele().getType(), value);
                }
            };

    private FilterFunction<PhenotypeAnnotation, String> sourceFilter =
            (annotation, value) -> {
                if (annotation.getSource() != null)
                    return FilterFunction.contains(annotation.getSource().getName(), value);
                return value == null;
            };


    private FilterFunction<PhenotypeAnnotation, String> referenceFilter =
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
        filterFieldMap.put(FieldFilter.SOURCE, sourceFilter);
    }

}

