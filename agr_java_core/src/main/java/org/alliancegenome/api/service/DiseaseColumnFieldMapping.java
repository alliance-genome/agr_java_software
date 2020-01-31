package org.alliancegenome.api.service;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.alliancegenome.api.service.Column.ASSOCIATES_GENES_ASSOCIATION;
import static org.alliancegenome.api.service.Column.ASSOCIATES_GENES_SPECIES;

public class DiseaseColumnFieldMapping extends ColumnFieldMapping<DiseaseAnnotation> {

    Map<Column, Function<DiseaseAnnotation, String>> mapColumnAttribute = new HashMap<>();

    public Map<Column, Function<DiseaseAnnotation, String>> getMapColumnAttribute() {
        return mapColumnAttribute;
    }

    public DiseaseColumnFieldMapping() {
        mapColumnFieldName.put(ASSOCIATES_GENES_SPECIES, FieldFilter.SPECIES);
        mapColumnFieldName.put(Column.ASSOCIATES_GENES_ASSOCIATION, FieldFilter.ASSOCIATION_TYPE);

        mapColumnAttribute.put(ASSOCIATES_GENES_SPECIES, entity -> entity.getGene().getSpecies().getName());
        mapColumnAttribute.put(ASSOCIATES_GENES_ASSOCIATION, DiseaseAnnotation::getAssociationType);

        singleValueDistinctFieldColumns.add(ASSOCIATES_GENES_SPECIES);
        singleValueDistinctFieldColumns.add(ASSOCIATES_GENES_ASSOCIATION);
    }

}
