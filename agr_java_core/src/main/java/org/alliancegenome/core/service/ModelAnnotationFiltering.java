package org.alliancegenome.core.service;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;

public class ModelAnnotationFiltering extends AnnotationFiltering<DiseaseAnnotation> {


    public FilterFunction<DiseaseAnnotation, String> modelNameFilter =
            (annotedEntity, value) -> FilterFunction.contains(annotedEntity.getModel().getNameText(), value);

    public FilterFunction<DiseaseAnnotation, String> termNameFilter =
            (annotedEntity, value) -> FilterFunction.contains(annotedEntity.getDisease().getName(), value);

    /*

    public FilterFunction<DiseaseAnnotation, String> sourceFilter =
            (annotation, value) -> FilterFunction.contains(annotation.getSource().getName(), value);

    public FilterFunction<DiseaseAnnotation, String> geneNameFilter =
            (annotation, value) -> FilterFunction.contains(annotation.getGene().getSymbol(), value);

*/
    public FilterFunction<DiseaseAnnotation, String> geneSpeciesFilter =
            (annotation, value) -> FilterFunction.fullMatchMultiValueOR(annotation.getModel().getSpecies().getName(), value);

    public ModelAnnotationFiltering() {
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

