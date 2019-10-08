package org.alliancegenome.core.service;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.neo4j.entity.node.Allele;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AlleleFiltering {


    /*
        public static FilterFunction<Allele, String> sourceFilter =
                (allele, value) -> FilterFunction.contains(allele.get.getName(), value);

    */
    public static FilterFunction<Allele, String> alleleFilter =
            (allele, value) -> FilterFunction.contains(allele.getSymbolText(), value);

/*
    public static FilterFunction<Allele, String> alleleFilter =
            (allele, value) -> FilterFunction.fullMatchMultiValueOR(allele.getGeneticEntityType(), value);
*/

    public static FilterFunction<Allele, String> synonymFilter =
            (allele, value) -> {
                Set<Boolean> filteringPassed = allele.getSynonyms().stream()
                        .map(synonym -> FilterFunction.contains(synonym.getName(), value))
                        .collect(Collectors.toSet());
                return !filteringPassed.isEmpty() && filteringPassed.contains(true);
            };

    public static FilterFunction<Allele, String> diseaseFilter =
            (allele, value) -> {
                Set<Boolean> filteringPassed = allele.getDiseases().stream()
                        .map(term -> FilterFunction.contains(term.getName(), value))
                        .collect(Collectors.toSet());
                return !filteringPassed.isEmpty() && filteringPassed.contains(true);
            };

    public static FilterFunction<Allele, String> variantTypeFilter =
            (allele, value) -> {
                Set<Boolean> filteringPassed = allele.getVariants().stream()
                        .map(term -> FilterFunction.contains(term.getType().getName(), value))
                        .collect(Collectors.toSet());
                return !filteringPassed.isEmpty() && filteringPassed.contains(true);
            };

    public static FilterFunction<Allele, String> variantConsequenceFilter =
            (allele, value) -> {
                Set<Boolean> filteringPassed = allele.getVariants().stream()
                        .map(term -> FilterFunction.contains(term.getGeneLevelConsequence().getGeneLevelConsequence().toLowerCase(), value))
                        .collect(Collectors.toSet());
                return !filteringPassed.isEmpty() && filteringPassed.contains(true);
            };

    public static FilterFunction<Allele, String> phenotypeFilter =
            (allele, value) -> {
                Set<Boolean> filteringPassed = allele.getPhenotypes().stream()
                        .map(term -> FilterFunction.contains(term.getPhenotypeStatement(), value))
                        .collect(Collectors.toSet());
                return !filteringPassed.isEmpty() && filteringPassed.contains(true);
            };

    public static Map<FieldFilter, FilterFunction<Allele, String>> filterFieldMap = new HashMap<>();

    static {
        filterFieldMap.put(FieldFilter.SYMBOL, alleleFilter);
        filterFieldMap.put(FieldFilter.SYNONYMS, synonymFilter);
        filterFieldMap.put(FieldFilter.PHENOTYPE, phenotypeFilter);
        filterFieldMap.put(FieldFilter.DISEASE, diseaseFilter);
        filterFieldMap.put(FieldFilter.VARIANT_TYPE, variantTypeFilter);
        filterFieldMap.put(FieldFilter.VARIANT_CONSEQUENCE, variantConsequenceFilter);
    }

    public static boolean isValidFiltering(Map<FieldFilter, String> fieldFilterValueMap) {
        if (fieldFilterValueMap == null)
            return true;
        Set<Boolean> result = fieldFilterValueMap.entrySet().stream()
                .map(entry -> filterFieldMap.containsKey(entry.getKey()))
                .collect(Collectors.toSet());
        return !result.isEmpty() && !result.contains(false);
    }

    public static List<String> getInvalidFieldFilter(Map<FieldFilter, String> fieldFilterValueMap) {
        return fieldFilterValueMap.entrySet().stream()
                .filter(entry -> !filterFieldMap.containsKey(entry.getKey()))
                .map(entry -> entry.getKey().getFullName())
                .collect(Collectors.toList());
    }

}

