package org.alliancegenome.api.service;

import java.util.Arrays;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@FunctionalInterface
public interface FilterFunction<Entity, FilterValue> {

    boolean containsFilterValue(Entity entity, FilterValue val);

    static boolean contains(String entityName, String value) {
        Objects.requireNonNull(entityName);
        if (value == null)
            return true;
        value = value.trim();
        String[] token = value.split(" ");
        Set<Boolean> resultSet = Arrays.stream(token)
                .map(val -> entityName.toLowerCase().contains(val.toLowerCase()))
                .collect(Collectors.toSet());
        return !resultSet.contains(false);
    }

}
