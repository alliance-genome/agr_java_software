package org.alliancegenome.cache.repository.helper;

import static org.alliancegenome.neo4j.entity.SpeciesType.NCBITAXON;

import java.util.Set;
import java.util.stream.Collectors;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.apache.commons.lang3.StringUtils;

public class ModelAnnotationFiltering extends AnnotationFiltering<DiseaseAnnotation> {


    public FilterFunction<DiseaseAnnotation, String> modelNameFilter =
            (annotedEntity, value) -> FilterFunction.contains(annotedEntity.getModel().getNameText(), value);

    public FilterFunction<DiseaseAnnotation, String> termNameFilter =
            (annotedEntity, value) -> FilterFunction.contains(annotedEntity.getDisease().getName(), value);

    public FilterFunction<DiseaseAnnotation, String> evidenceCodeFilter =
            (annotation, value) -> {
                Set<Boolean> filteringPassed = annotation.getEcoCodes().stream()
                        // this encodes the logic:
                        // if there is a displaySynonym (three / four-letter abbrev then check that attribute
                        // otherwise check the term name
                        .map(evidenceCode -> {
                            if (StringUtils.isNotEmpty(evidenceCode.getDisplaySynonym()))
                                return FilterFunction.contains(evidenceCode.getDisplaySynonym(), value);
                            else
                                return FilterFunction.contains(evidenceCode.getName(), value);
                        })
                        .collect(Collectors.toSet());
                return !filteringPassed.contains(false);
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

    public FilterFunction<DiseaseAnnotation, String> associationTypeFilter =
            (annotation, value) -> FilterFunction.contains(annotation.getAssociationType(), value);

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
        filterFieldMap.put(FieldFilter.ASSOCIATION_TYPE, associationTypeFilter);
    }

}

