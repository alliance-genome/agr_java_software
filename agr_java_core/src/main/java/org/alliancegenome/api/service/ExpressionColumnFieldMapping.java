package org.alliancegenome.api.service;

import static org.alliancegenome.api.service.Column.EXPRESSION_SPECIES;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.alliancegenome.core.ExpressionDetail;
import org.alliancegenome.es.model.query.FieldFilter;

public class ExpressionColumnFieldMapping extends ColumnFieldMapping<ExpressionDetail> {

	private Map<Column, Function<ExpressionDetail, Set<String>>> mapColumnAttribute = new HashMap<>();

	public Map<Column, Function<ExpressionDetail, Set<String>>> getMapColumnAttribute() {
		return mapColumnAttribute;
	}

	public ExpressionColumnFieldMapping() {
		mapColumnFieldName.put(EXPRESSION_SPECIES, FieldFilter.SPECIES);

		mapColumnAttribute.put(EXPRESSION_SPECIES, entity -> Set.of(entity.getGene().getSpecies().getName()));

		singleValueDistinctFieldColumns.add(EXPRESSION_SPECIES);
	}

}
