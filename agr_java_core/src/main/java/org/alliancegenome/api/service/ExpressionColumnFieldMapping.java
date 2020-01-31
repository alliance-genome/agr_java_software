package org.alliancegenome.api.service;

import org.alliancegenome.core.ExpressionDetail;
import org.alliancegenome.es.model.query.FieldFilter;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.alliancegenome.api.service.Column.EXPRESSION_SPECIES;

public class ExpressionColumnFieldMapping extends ColumnFieldMapping<ExpressionDetail> {

    private Map<Column, Function<ExpressionDetail, String>> mapColumnAttribute = new HashMap<>();

    public Map<Column, Function<ExpressionDetail, String>> getMapColumnAttribute() {
        return mapColumnAttribute;
    }

    public ExpressionColumnFieldMapping() {
        mapColumnFieldName.put(EXPRESSION_SPECIES, FieldFilter.SPECIES);

        mapColumnAttribute.put(EXPRESSION_SPECIES, entity -> entity.getGene().getSpecies().getName());

        singleValueDistinctFieldColumns.add(EXPRESSION_SPECIES);
    }

}
