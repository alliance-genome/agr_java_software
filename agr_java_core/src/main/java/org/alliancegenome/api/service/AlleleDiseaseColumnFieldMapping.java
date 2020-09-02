package org.alliancegenome.api.service;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.alliancegenome.api.service.Column.*;

public class AlleleDiseaseColumnFieldMapping extends ColumnFieldMapping<DiseaseAnnotation> {

    private Map<Column, Function<DiseaseAnnotation, Set<String>>> mapColumnAttribute = new HashMap<>();

    public Map<Column, Function<DiseaseAnnotation, Set<String>>> getMapColumnAttribute() {
        return mapColumnAttribute;
    }

    public AlleleDiseaseColumnFieldMapping() {
        mapColumnFieldName.put(ASSOCIATION_TYPE, FieldFilter.ASSOCIATION_TYPE);

        mapColumnAttribute.put(ASSOCIATION_TYPE, entity -> Set.of(entity.getAssociationType()));

        singleValueDistinctFieldColumns.add(ASSOCIATION_TYPE);
    }

}
