package org.alliancegenome.core.service;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.Gene;

import java.util.Set;
import java.util.stream.Collectors;

public class PrimaryAnnotatedEntityFiltering extends AnnotationFiltering<PrimaryAnnotatedEntity> {


    public FilterFunction<PrimaryAnnotatedEntity, String> modelNameFilter =
            (annotedEntity, value) -> FilterFunction.contains(annotedEntity.getName(), value);

    public FilterFunction<PrimaryAnnotatedEntity, String> termNameFilter =
            (annotedEntity, value) -> FilterFunction.contains(annotedEntity.getDisease().getName(), value);

    /*

    public FilterFunction<PrimaryAnnotatedEntity, String> sourceFilter =
            (annotation, value) -> FilterFunction.contains(annotation.getSource().getName(), value);

    public FilterFunction<PrimaryAnnotatedEntity, String> geneNameFilter =
            (annotation, value) -> FilterFunction.contains(annotation.getGene().getSymbol(), value);

*/
    public FilterFunction<PrimaryAnnotatedEntity, String> geneSpeciesFilter =
            (annotation, value) -> FilterFunction.fullMatchMultiValueOR(annotation.getSpecies().getName(), value);

/*
    public FilterFunction<PrimaryAnnotatedEntity, String> evidenceCodeFilter =
            (annotation, value) -> {
                Set<Boolean> filteringPassed = annotation.getEcoCodes().stream()
                        .map(evidenceCode -> FilterFunction.contains(evidenceCode.getName(), value))
                        .collect(Collectors.toSet());
                return !filteringPassed.contains(false);
            };

    public FilterFunction<PrimaryAnnotatedEntity, String> referenceFilter =
            (annotation, value) -> {
                Set<Boolean> filteringPassed = annotation.getPublications().stream()
                        .map(publication -> FilterFunction.contains(publication.getPubId(), value))
                        .collect(Collectors.toSet());
                // return true if at least one pub is found
                return filteringPassed.contains(true);
            };

*/
    public PrimaryAnnotatedEntityFiltering() {
/*
        filterFieldMap.put(FieldFilter.EVIDENCE_CODE, evidenceCodeFilter);
        filterFieldMap.put(FieldFilter.FREFERENCE, referenceFilter);
        filterFieldMap.put(FieldFilter.SOURCE, sourceFilter);
        filterFieldMap.put(FieldFilter.GENE_NAME, geneNameFilter);
*/
        filterFieldMap.put(FieldFilter.DISEASE, termNameFilter);
        filterFieldMap.put(FieldFilter.MODEL_NAME, modelNameFilter);
        filterFieldMap.put(FieldFilter.SPECIES, geneSpeciesFilter);
    }

}

