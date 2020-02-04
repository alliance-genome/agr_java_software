package org.alliancegenome.api.service;

import org.alliancegenome.es.model.query.FieldFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public abstract class ColumnFieldMapping<T> {

    Map<Column, FieldFilter> mapColumnFieldName = new HashMap<>();

    List<Column> singleValueDistinctFieldColumns = new ArrayList<>();

    public abstract Map<Column, Function<T, String>> getMapColumnAttribute();

    public Map<Column, Function<T, String>> getSingleValuedFieldColumns(Table table) {
        Map<Column, Function<T, String>> map = new HashMap<>();
        singleValueDistinctFieldColumns.stream()
                .filter(column -> column.getTable().equals(table))
                .forEach(column -> getMapColumnAttribute().entrySet().stream()
                        .filter(entry -> entry.getKey().equals(column))
                        .forEach(entry -> map.put(entry.getKey(), entry.getValue())));
        return map;
    }

    String getFieldFilterName(Column column) {
        return mapColumnFieldName.get(column).getName();
    }

    public Map<Column, FieldFilter> getMapColumnFieldName() {
        return mapColumnFieldName;
    }

    public List<FieldFilter> getColumnFieldFilters() {
        return mapColumnFieldName.entrySet().stream()
                .filter(entry -> entry.getKey().isFilterElement())
                .map(Map.Entry::getValue)
                .collect(Collectors.toList());
    }

    public String getAllowedFieldFilterNames() {
        return mapColumnFieldName.entrySet().stream()
                .filter(entry -> entry.getKey().isFilterElement())
                .map(Map.Entry::getValue)
                .collect(Collectors.toList()).stream()
                .map(FieldFilter::getName)
                .collect(Collectors.joining(", "));
    }
}
