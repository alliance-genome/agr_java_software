package org.alliancegenome.api.service;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.node.Gene;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.alliancegenome.api.service.FilterFunction.contains;

public class DiseaseAnnotationFiltering {


    public static FilterFunction<DiseaseAnnotation, String> termNameFilter =
            (annotation, value) -> contains(annotation.getDisease().getName(), value);

    public static FilterFunction<DiseaseAnnotation, String> associationFilter =
            (annotation, value) -> contains(annotation.getAssociationType(), value);

    public static FilterFunction<DiseaseAnnotation, String> sourceFilter =
            (annotation, value) -> contains(annotation.getSource().getName(), value);

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
                return !filteringPassed.contains(false);
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
    }

}

