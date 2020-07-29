package org.alliancegenome.cache.repository.helper;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.neo4j.entity.node.Allele;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class AlleleFiltering extends AnnotationFiltering<Allele> {


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
            (allele, value) ->
                    FilterFunction.fullMatchMultiValueOR(allele.getVariants().stream()
                            .filter(Objects::nonNull)
                            .map(variant -> variant.getType().getName())
                            .collect(Collectors.toSet()), value);

    public static FilterFunction<Allele, String> variantConsequenceFilter =
            (allele, value) ->
                    FilterFunction.fullMatchMultiValueOR(allele.getVariants().stream()
                            .filter(Objects::nonNull)
                            .filter(variant -> variant.getGeneLevelConsequence() != null)
                            .map(variant -> variant.getGeneLevelConsequence().getGeneLevelConsequence())
                            .collect(Collectors.toSet()), value);
    ;

    public static FilterFunction<Allele, String> transgenicAlleleConstructFilter =
            (allele, value) ->
                    FilterFunction.fullMatchMultiValueOR(allele.getConstructs().stream()
                            .filter(Objects::nonNull)
                            .map(construct -> construct.getNameText())
                            .collect(Collectors.toSet()), value);
    ;

    public static FilterFunction<Allele, String> phenotypeFilter =
            (allele, value) -> {
                Set<Boolean> filteringPassed = allele.getPhenotypes().stream()
                        .map(term -> FilterFunction.contains(term.getPhenotypeStatement(), value))
                        .collect(Collectors.toSet());
                return !filteringPassed.isEmpty() && filteringPassed.contains(true);
            };

    public AlleleFiltering() {
        filterFieldMap.put(FieldFilter.SYMBOL, alleleFilter);
        filterFieldMap.put(FieldFilter.SYNONYMS, synonymFilter);
        filterFieldMap.put(FieldFilter.PHENOTYPE, phenotypeFilter);
        filterFieldMap.put(FieldFilter.DISEASE, diseaseFilter);
        filterFieldMap.put(FieldFilter.VARIANT_TYPE, variantTypeFilter);
        filterFieldMap.put(FieldFilter.VARIANT_CONSEQUENCE, variantConsequenceFilter);
        filterFieldMap.put(FieldFilter.CONSTRUCT_SYMBOL, transgenicAlleleConstructFilter);
    }

}

