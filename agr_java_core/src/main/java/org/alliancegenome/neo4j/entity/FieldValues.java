package org.alliancegenome.neo4j.entity;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.alliancegenome.es.model.query.FieldFilter;

public abstract class FieldValues<T> {

    public Map<FieldFilter, Function<T, String>> filterFields = new HashMap<>();

    public Map<FieldFilter, Function<T, String>> getFields() {
        return filterFields;
    }
}
