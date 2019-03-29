package org.alliancegenome.api.service;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.node.Gene;

import java.util.*;
import java.util.stream.Collectors;

import static org.alliancegenome.api.service.FilterFunction.contains;
import static org.alliancegenome.api.service.FilterFunction.fullMatchMultiValueOR;

public class DiseaseAnnotationFiltering {


    public static FilterFunction<DiseaseAnnotation, String> termNameFilter =
            (annotation, value) -> contains(annotation.getDisease().getName(), value);

    public static FilterFunction<DiseaseAnnotation, String> geneticEntityFilter =
            (annotation, value) -> {
                if (annotation.getFeature() == null)
                    return false;
                return contains(annotation.getFeature().getSymbolText(), value);
            };

    public static FilterFunction<DiseaseAnnotation, String> associationFilter =
            (annotation, value) -> contains(annotation.getAssociationType(), value);

    public static FilterFunction<DiseaseAnnotation, String> sourceFilter =
            (annotation, value) -> contains(annotation.getSource().getName(), value);

    public static FilterFunction<DiseaseAnnotation, String> geneticEntityTypeFilter =
            (annotation, value) -> FilterFunction.fullMatchMultiValueOR(annotation.getGeneticEntityType(), value);

    public static FilterFunction<DiseaseAnnotation, String> geneNameFilter =
            (annotation, value) -> contains(annotation.getGene().getSymbol(), value);

    public static FilterFunction<DiseaseAnnotation, String> geneSpeciesFilter =
            (annotation, value) -> contains(annotation.getGene().getSpecies().getName(), value);

    public static FilterFunction<DiseaseAnnotation, String> evidenceCodeFilter =
            (annotation, value) -> {
                Set<Boolean> filteringPassed = annotation.getEvidenceCodes().stream()
                        .map(evidenceCode -> contains(evidenceCode.getName(), value))
                        .collect(Collectors.toSet());
                return !filteringPassed.contains(false);
            };

    public static FilterFunction<DiseaseAnnotation, String> referenceFilter =
            (annotation, value) -> {
                Set<Boolean> filteringPassed = annotation.getPublications().stream()
                        .map(publication -> contains(publication.getPubId(), value))
                        .collect(Collectors.toSet());
                // return true if at least one pub is found
                return filteringPassed.contains(true);
            };

    public static FilterFunction<DiseaseAnnotation, String> orthologFilter =
            (annotation, value) -> {
                Gene orthologyGene = annotation.getOrthologyGene();
                if (orthologyGene == null)
                    return false;
                return contains(orthologyGene.getSymbol(), value);
            };

    public static FilterFunction<DiseaseAnnotation, String> orthologSpeciesFilter =
            (annotation, value) -> {
                Gene orthologyGene = annotation.getOrthologyGene();
                if (orthologyGene == null)
                    return false;
                return contains(orthologyGene.getSpecies().getName(), value);
            };


    public static Map<FieldFilter, FilterFunction<DiseaseAnnotation, String>> filterFieldMap = new HashMap<>();

    static {
        filterFieldMap.put(FieldFilter.DISEASE, termNameFilter);
        filterFieldMap.put(FieldFilter.ASSOCIATION_TYPE, associationFilter);
        filterFieldMap.put(FieldFilter.ORTHOLOG, orthologFilter);
        filterFieldMap.put(FieldFilter.ORTHOLOG_SPECIES, orthologSpeciesFilter);
        filterFieldMap.put(FieldFilter.EVIDENCE_CODE, evidenceCodeFilter);
        filterFieldMap.put(FieldFilter.REFERENCE, referenceFilter);
        filterFieldMap.put(FieldFilter.SOURCE, sourceFilter);
        filterFieldMap.put(FieldFilter.GENETIC_ENTITY_TYPE, geneticEntityTypeFilter);
        filterFieldMap.put(FieldFilter.GENETIC_ENTITY, geneticEntityFilter);
        filterFieldMap.put(FieldFilter.GENE_NAME, geneNameFilter);
        filterFieldMap.put(FieldFilter.SPECIES, geneSpeciesFilter);
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

