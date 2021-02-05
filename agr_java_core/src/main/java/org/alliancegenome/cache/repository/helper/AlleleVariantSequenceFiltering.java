package org.alliancegenome.cache.repository.helper;

import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.es.model.query.FieldFilter;

import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class AlleleVariantSequenceFiltering extends AnnotationFiltering<AlleleVariantSequence> {


    private static final FilterFunction<AlleleVariantSequence, String> alleleFilter =
            (allele, value) -> FilterFunction.contains(allele.getAllele().getSymbolText(), value);

    private static final FilterFunction<AlleleVariantSequence, String> alleleCategoryFilter =
            (allele, value) -> FilterFunction.fullMatchMultiValueOR(allele.getAllele().getCategory(), value);

    private static final FilterFunction<AlleleVariantSequence, String> synonymFilter =
            (allele, value) -> {
                Set<Boolean> filteringPassed = allele.getAllele().getSynonyms().stream()
                        .map(synonym -> FilterFunction.contains(synonym.getName(), value))
                        .collect(Collectors.toSet());
                return !filteringPassed.isEmpty() && filteringPassed.contains(true);
            };

    private static final FilterFunction<AlleleVariantSequence, String> variantTypeFilter =
            (allele, value) ->
                    FilterFunction.fullMatchMultiValueOR(allele.getAllele().getVariants().stream()
                            .filter(Objects::nonNull)
                            .map(variant -> variant.getVariantType().getName())
                            .collect(Collectors.toSet()), value);

    private static final FilterFunction<AlleleVariantSequence, String> alleleHasPhenotypeFilter =
            (allele, value) ->
                    FilterFunction.fullMatchMultiValueOR(allele.getAllele().hasPhenotype().toString(), value);

    private static final FilterFunction<AlleleVariantSequence, String> alleleHasDiseaseFilter =
            (allele, value) ->
                    FilterFunction.fullMatchMultiValueOR(allele.getAllele().hasDisease().toString(), value);

    private static final FilterFunction<AlleleVariantSequence, String> molecularConsequenceFilter =
            (allele, value) ->
                    FilterFunction.fullMatchMultiValueOR(allele.getAllele().getVariants().stream()
                            .filter(Objects::nonNull)
                            .filter(variant -> variant.getGeneLevelConsequence() != null)
                            .map(variant -> variant.getGeneLevelConsequence().getGeneLevelConsequence())
                            .collect(Collectors.toSet()), value);

    private static final FilterFunction<AlleleVariantSequence, String> variantImpactFilter =
            (allele, value) -> {
                if (allele.getConsequence() != null)
                    return FilterFunction.fullMatchMultiValueOR(allele.getConsequence().getImpact(), value);
                return false;
            };

    private static final FilterFunction<AlleleVariantSequence, String> sequenceFeatureTypeFilter =
            (allele, value) -> {
                if (allele.getConsequence() != null)
                    return FilterFunction.fullMatchMultiValueOR(allele.getConsequence().getSequenceFeatureType(), value);
                return false;
            };

    private static final FilterFunction<AlleleVariantSequence, String> variantPolyphenFilter =
            (allele, value) -> {
                if (allele.getConsequence() != null)
                    return FilterFunction.fullMatchMultiValueOR(allele.getConsequence().getPolyphenPrediction(), value);
                return false;
            };

    private static final FilterFunction<AlleleVariantSequence, String> variantSiftFilter =
            (allele, value) -> {
                if (allele.getConsequence() != null)
                    return FilterFunction.fullMatchMultiValueOR(allele.getConsequence().getSiftPrediction(), value);
                return false;
            };


    public AlleleVariantSequenceFiltering() {
        filterFieldMap.put(FieldFilter.SYMBOL, alleleFilter);
        filterFieldMap.put(FieldFilter.SYNONYMS, synonymFilter);
        filterFieldMap.put(FieldFilter.ALLELE_CATEGORY, alleleCategoryFilter);
        filterFieldMap.put(FieldFilter.HAS_PHENOTYPE, alleleHasPhenotypeFilter);
        filterFieldMap.put(FieldFilter.HAS_DISEASE, alleleHasDiseaseFilter);
        filterFieldMap.put(FieldFilter.VARIANT_TYPE, variantTypeFilter);
        filterFieldMap.put(FieldFilter.MOLECULAR_CONSEQUENCE, molecularConsequenceFilter);
        filterFieldMap.put(FieldFilter.VARIANT_IMPACT, variantImpactFilter);
        filterFieldMap.put(FieldFilter.VARIANT_POLYPHEN, variantPolyphenFilter);
        filterFieldMap.put(FieldFilter.VARIANT_SIFT, variantSiftFilter);
        filterFieldMap.put(FieldFilter.SEQUENCE_FEATURE_TYPE, sequenceFeatureTypeFilter);
        filterFieldMap.put(FieldFilter.VARIANT_TYPE, variantTypeFilter);
    }

}

