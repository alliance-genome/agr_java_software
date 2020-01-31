package org.alliancegenome.api.service;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.alliancegenome.api.service.Column.*;

public class DiseaseColumnFieldMapping extends ColumnFieldMapping<DiseaseAnnotation> {

    Map<Column, Function<DiseaseAnnotation, String>> mapColumnAttribute = new HashMap<>();

    public Map<Column, Function<DiseaseAnnotation, String>> getMapColumnAttribute() {
        return mapColumnAttribute;
    }

    public DiseaseColumnFieldMapping() {
        mapColumnFieldName.put(ASSOCIATES_GENES_SPECIES, FieldFilter.SPECIES);
        mapColumnFieldName.put(ASSOCIATES_GENES_ASSOCIATION, FieldFilter.ASSOCIATION_TYPE);
        mapColumnFieldName.put(ALLELE_SPECIES, FieldFilter.SPECIES);
        mapColumnFieldName.put(ALLELE_ASSOCIATION, FieldFilter.ASSOCIATION_TYPE);
        mapColumnFieldName.put(MODEL_SPECIES, FieldFilter.SPECIES);

        mapColumnAttribute.put(ASSOCIATES_GENES_SPECIES, entity -> entity.getGene().getSpecies().getName());
        mapColumnAttribute.put(ASSOCIATES_GENES_ASSOCIATION, DiseaseAnnotation::getAssociationType);
        mapColumnAttribute.put(ALLELE_SPECIES, entity -> entity.getGene().getSpecies().getName());
        mapColumnAttribute.put(ALLELE_ASSOCIATION, DiseaseAnnotation::getAssociationType);
        mapColumnAttribute.put(MODEL_SPECIES, entity -> entity.getModel().getSpecies().getName());

        singleValueDistinctFieldColumns.add(ASSOCIATES_GENES_SPECIES);
        singleValueDistinctFieldColumns.add(ASSOCIATES_GENES_ASSOCIATION);
        singleValueDistinctFieldColumns.add(ALLELE_SPECIES);
        singleValueDistinctFieldColumns.add(ALLELE_ASSOCIATION);
        singleValueDistinctFieldColumns.add(MODEL_SPECIES);
    }

}
