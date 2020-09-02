package org.alliancegenome.api.service;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.alliancegenome.api.service.Column.*;

public class DiseaseColumnFieldMapping extends ColumnFieldMapping<DiseaseAnnotation> {

    private Map<Column, Function<DiseaseAnnotation, Set<String>>> mapColumnAttribute = new HashMap<>();

    public Map<Column, Function<DiseaseAnnotation, Set<String>>> getMapColumnAttribute() {
        return mapColumnAttribute;
    }

    public DiseaseColumnFieldMapping() {
        mapColumnFieldName.put(ASSOCIATES_GENES_SPECIES, FieldFilter.SPECIES);
        mapColumnFieldName.put(ASSOCIATES_GENES_ASSOCIATION, FieldFilter.ASSOCIATION_TYPE);
        mapColumnFieldName.put(ALLELE_SPECIES, FieldFilter.SPECIES);
        mapColumnFieldName.put(ALLELE_ASSOCIATION, FieldFilter.ASSOCIATION_TYPE);
        mapColumnFieldName.put(MODEL_SPECIES, FieldFilter.SPECIES);
        mapColumnFieldName.put(MODEL_ASSOCIATION_TYPE, FieldFilter.ASSOCIATION_TYPE);
        mapColumnFieldName.put(DISEASE_SPECIES, FieldFilter.SPECIES);
        mapColumnFieldName.put(DISEASE_ASSOCIATION, FieldFilter.ASSOCIATION_TYPE);

        mapColumnAttribute.put(ASSOCIATES_GENES_SPECIES, entity -> Set.of(entity.getGene().getSpecies().getName()));
        mapColumnAttribute.put(MODEL_ASSOCIATION_TYPE, entity -> Set.of(entity.getAssociationType()));
        mapColumnAttribute.put(ASSOCIATES_GENES_ASSOCIATION, entity -> Set.of(entity.getAssociationType()));
        mapColumnAttribute.put(ALLELE_SPECIES, entity -> Set.of(entity.getFeature().getSpecies().getName()));
        mapColumnAttribute.put(ALLELE_ASSOCIATION, entity -> Set.of(entity.getAssociationType()));
        mapColumnAttribute.put(MODEL_SPECIES, entity -> Set.of(entity.getModel().getSpecies().getName()));
        mapColumnAttribute.put(DISEASE_SPECIES, entity -> Set.of(entity.getGene().getSpecies().getName()));
        mapColumnAttribute.put(DISEASE_ASSOCIATION, entity -> Set.of(entity.getAssociationType()));

        singleValueDistinctFieldColumns.add(ASSOCIATES_GENES_SPECIES);
        singleValueDistinctFieldColumns.add(ASSOCIATES_GENES_ASSOCIATION);
        singleValueDistinctFieldColumns.add(ALLELE_SPECIES);
        singleValueDistinctFieldColumns.add(ALLELE_ASSOCIATION);
        singleValueDistinctFieldColumns.add(MODEL_SPECIES);
        singleValueDistinctFieldColumns.add(MODEL_ASSOCIATION_TYPE);
        singleValueDistinctFieldColumns.add(DISEASE_SPECIES);
        singleValueDistinctFieldColumns.add(DISEASE_ASSOCIATION);
    }

}
