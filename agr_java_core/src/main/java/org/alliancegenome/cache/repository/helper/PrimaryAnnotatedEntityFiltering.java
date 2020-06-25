package org.alliancegenome.cache.repository.helper;

import java.util.Set;
import java.util.stream.Collectors;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.apache.commons.collections4.CollectionUtils;

public class PrimaryAnnotatedEntityFiltering extends AnnotationFiltering<PrimaryAnnotatedEntity> {


    public FilterFunction<PrimaryAnnotatedEntity, String> modelNameFilter =
            (annotedEntity, value) -> FilterFunction.contains(annotedEntity.getDisplayName(), value);

    public FilterFunction<PrimaryAnnotatedEntity, String> termNameFilter =
            (annotation, value) -> {
                if (CollectionUtils.isEmpty(annotation.getDiseases()))
                    return false;
                Set<Boolean> filteringPassed = annotation.getDiseases().stream()
                        .map(disease -> FilterFunction.contains(disease.getName(), value))
                        .collect(Collectors.toSet());
                return filteringPassed.contains(true);
            };

    public FilterFunction<PrimaryAnnotatedEntity, String> phenotypeFilter =
            (annotation, value) -> {
                if (annotation.getPhenotypes() == null)
                    return false;
                Set<Boolean> filteringPassed = annotation.getPhenotypes().stream()
                        .map(phenotype -> FilterFunction.contains(phenotype, value))
                        .collect(Collectors.toSet());
                return filteringPassed.contains(true);
            };

    private FilterFunction<PrimaryAnnotatedEntity, String> sourceFilter =
            (annotation, value) -> {
                if (annotation.getSource() != null)
                    return FilterFunction.contains(annotation.getSource().getName(), value);
                return value == null;
            };


    public PrimaryAnnotatedEntityFiltering() {
/*
        filterFieldMap.put(FieldFilter.EVIDENCE_CODE, evidenceCodeFilter);
        filterFieldMap.put(FieldFilter.FREFERENCE, referenceFilter);
        filterFieldMap.put(FieldFilter.INERACTION_SOURCE, sourceFilter);
        filterFieldMap.put(FieldFilter.GENE_NAME, geneNameFilter);
*/
        filterFieldMap.put(FieldFilter.DISEASE, termNameFilter);
        filterFieldMap.put(FieldFilter.PHENOTYPE, phenotypeFilter);
        filterFieldMap.put(FieldFilter.MODEL_NAME, modelNameFilter);
        filterFieldMap.put(FieldFilter.SOURCE, sourceFilter);
    }

}

