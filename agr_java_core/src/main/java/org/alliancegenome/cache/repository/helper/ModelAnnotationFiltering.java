package org.alliancegenome.cache.repository.helper;

import java.util.Set;
import java.util.stream.Collectors;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;

import static org.alliancegenome.neo4j.entity.SpeciesType.NCBITAXON;

public class ModelAnnotationFiltering extends AnnotationFiltering<DiseaseAnnotation> {


    public FilterFunction<DiseaseAnnotation, String> modelNameFilter =
            (annotedEntity, value) -> FilterFunction.contains(annotedEntity.getModel().getNameText(), value);

    public FilterFunction<DiseaseAnnotation, String> termNameFilter =
            (annotedEntity, value) -> FilterFunction.contains(annotedEntity.getDisease().getName(), value);

    public FilterFunction<DiseaseAnnotation, String> evidenceCodeFilter =
            (annotation, value) -> {
                Set<Boolean> filteringPassed = annotation.getEcoCodes().stream()
                        .map(evidenceCode -> FilterFunction.contains(evidenceCode.getName(), value))
                        .collect(Collectors.toSet());
                return filteringPassed.contains(true);
            };
            
    public FilterFunction<DiseaseAnnotation, String> referenceFilter =
            (annotation, value) -> {
                Set<Boolean> filteringPassed = annotation.getPublications().stream()
                        .map(publication -> FilterFunction.contains(publication.getPubId(), value))
                        .collect(Collectors.toSet());
                // return true if at least one pub is found
                return filteringPassed.contains(true);
            };
                    
    public FilterFunction<DiseaseAnnotation, String> sourceFilter =
            (annotation, value) -> FilterFunction.contains(annotation.getSource().getName(), value);

    public FilterFunction<DiseaseAnnotation, String> geneNameFilter =
            (annotation, value) -> FilterFunction.contains(annotation.getGene().getSymbol(), value);

    public FilterFunction<DiseaseAnnotation, String> geneSpeciesFilter =
            (annotation, value) -> {
                if (value.startsWith(NCBITAXON))
                    return FilterFunction.fullMatchMultiValueOR(annotation.getModel().getSpecies().getType().getTaxonID(), value);
                else
                    return FilterFunction.fullMatchMultiValueOR(annotation.getModel().getSpecies().getName(), value);
            };

    public ModelAnnotationFiltering() {
        filterFieldMap.put(FieldFilter.EVIDENCE_CODE, evidenceCodeFilter);
        filterFieldMap.put(FieldFilter.FREFERENCE, referenceFilter);
        filterFieldMap.put(FieldFilter.SOURCE, sourceFilter);
        filterFieldMap.put(FieldFilter.GENE_NAME, geneNameFilter);
        filterFieldMap.put(FieldFilter.DISEASE, termNameFilter);
        filterFieldMap.put(FieldFilter.MODEL_NAME, modelNameFilter);
        filterFieldMap.put(FieldFilter.SPECIES, geneSpeciesFilter);
    }

}

