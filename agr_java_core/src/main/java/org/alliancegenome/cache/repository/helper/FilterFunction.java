package org.alliancegenome.cache.repository.helper;

import java.util.*;
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
                .map(val -> entityName.trim().toLowerCase().contains(val.toLowerCase()))
                .collect(Collectors.toSet());
        return !resultSet.contains(false);
    }

    static boolean fullMatchMultiValueOR(String entity, String value) {
        return fullMatchMultiValueOR(entity, value, "\\|");
    }

    static boolean fullMatchMultiValueOR(Set<String> entity, String value) {
        return fullMatchMultiValueOR(entity, value, "\\|");
    }

    // List of values should match exactly the entity
    // but in an OR connector
    static boolean fullMatchMultiValueOR(String entity, String value, String delimiter) {
        System.out.println(entity+"\t-"+ value);
        String[] tokenList = value.split(delimiter);
        List<String> cleanedValues = Arrays.stream(tokenList)
                .map(s -> s.toLowerCase().trim())
                .collect(Collectors.toList());
        cleanedValues.removeIf(String::isEmpty);
        if (cleanedValues.isEmpty())
            return true;
        return cleanedValues.contains(entity.trim().toLowerCase());
    }

    static boolean fullMatchMultiValueOR(Set<String> entities, String value, String delimiter) {
        String[] tokenList = value.split(delimiter);
        List<String> cleanedValues = Arrays.stream(tokenList)
                .map(s -> s.toLowerCase().trim())
                .collect(Collectors.toList());
        cleanedValues.removeIf(String::isEmpty);
        if (cleanedValues.isEmpty())
            return true;
        return entities.stream().anyMatch(entity -> cleanedValues.contains(entity.toLowerCase()));
    }


}
