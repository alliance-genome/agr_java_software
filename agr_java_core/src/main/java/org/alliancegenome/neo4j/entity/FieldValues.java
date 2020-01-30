package org.alliancegenome.neo4j.entity;

import org.alliancegenome.es.model.query.FieldFilter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public abstract class FieldValues<T> {

    public Map<FieldFilter, Function<T, String>> filterFields = new HashMap<>();

    public Map<FieldFilter, Function<T, String>> getFields() {
        return filterFields;
    }
}
