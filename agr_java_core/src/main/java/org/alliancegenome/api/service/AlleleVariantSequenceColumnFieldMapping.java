package org.alliancegenome.api.service;

import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.es.model.query.FieldFilter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.alliancegenome.api.service.Column.*;

public class AlleleVariantSequenceColumnFieldMapping extends ColumnFieldMapping<AlleleVariantSequence> {

    private Map<Column, Function<AlleleVariantSequence, Set<String>>> mapColumnAttribute = new HashMap<>();

    public Map<Column, Function<AlleleVariantSequence, Set<String>>> getMapColumnAttribute() {
        return mapColumnAttribute;
    }

    public AlleleVariantSequenceColumnFieldMapping() {
        mapColumnFieldName.put(GENE_ALLELE_VARIANT_SEQUENCE_TYPE, FieldFilter.VARIANT_TYPE);
        mapColumnFieldName.put(GENE_ALLELE_VARIANT_SEQUENCE_CONSEQUENCE, FieldFilter.VARIANT_CONSEQUENCE);
        mapColumnFieldName.put(GENE_ALLELE_VARIANT_SEQUENCE_CATEGORY, FieldFilter.ALLELE_CATEGORY);
        mapColumnFieldName.put(GENE_ALLELE_VARIANT_SEQUENCE_IMPACT, FieldFilter.VARIANT_IMPACT);
        mapColumnFieldName.put(GENE_ALLELE_VARIANT_SEQUENCE_CONSEQUENCE_TYPE, FieldFilter.CONSEQUENCE_TYPE);
        mapColumnFieldName.put(GENE_ALLELE_VARIANT_SEQUENCE_VARIANT_POLYPHEN, FieldFilter.VARIANT_POLYPHEN);
        mapColumnFieldName.put(GENE_ALLELE_VARIANT_SEQUENCE_VARIANT_SIFT, FieldFilter.VARIANT_SIFT);

        mapColumnAttribute.put(GENE_ALLELE_VARIANT_SEQUENCE_CATEGORY, entity -> Set.of(entity.getAllele().getCategory()));
        mapColumnAttribute.put(GENE_ALLELE_VARIANT_SEQUENCE_TYPE, entity -> {
            if (entity.getVariant() != null) {
                return Set.of(entity.getVariant().getVariantType().getName());
            }
            return new HashSet<>();
        });
        mapColumnAttribute.put(GENE_ALLELE_VARIANT_SEQUENCE_CONSEQUENCE, entity -> {
            if (entity.getVariant() != null) {
                if (entity.getConsequence() != null)
                    return Set.of(entity.getConsequence().getTranscriptLevelConsequence());
            }
            return new HashSet<>();
        });
        mapColumnAttribute.put(GENE_ALLELE_VARIANT_SEQUENCE_IMPACT, entity -> {
            if (entity.getVariant() != null) {
                if (entity.getConsequence() != null)
                    return Set.of(entity.getConsequence().getImpact());
            }
            return new HashSet<>();
        });

        mapColumnAttribute.put(GENE_ALLELE_VARIANT_SEQUENCE_CONSEQUENCE_TYPE, entity -> {
            if (entity.getVariant() != null) {
                if (entity.getConsequence() != null)
                    return Set.of(entity.getConsequence().getTranscriptType());
            }
            return new HashSet<>();
        });

        mapColumnAttribute.put(GENE_ALLELE_VARIANT_SEQUENCE_VARIANT_POLYPHEN, entity -> {
            if (entity.getVariant() != null) {
                if (entity.getConsequence() != null && entity.getConsequence().getPolyphenPrediction() != null)
                    return Set.of(entity.getConsequence().getPolyphenPrediction());
            }
            return new HashSet<>();
        });

        mapColumnAttribute.put(GENE_ALLELE_VARIANT_SEQUENCE_VARIANT_SIFT, entity -> {
            if (entity.getVariant() != null) {
                if (entity.getConsequence() != null && entity.getConsequence().getSiftPrediction() != null)
                    return Set.of(entity.getConsequence().getSiftPrediction());
            }
            return new HashSet<>();
        });

        singleValueDistinctFieldColumns.add(GENE_ALLELE_VARIANT_SEQUENCE_TYPE);
        singleValueDistinctFieldColumns.add(GENE_ALLELE_VARIANT_SEQUENCE_CONSEQUENCE);
        singleValueDistinctFieldColumns.add(GENE_ALLELE_VARIANT_SEQUENCE_CATEGORY);
        singleValueDistinctFieldColumns.add(GENE_ALLELE_VARIANT_SEQUENCE_IMPACT);
        singleValueDistinctFieldColumns.add(GENE_ALLELE_VARIANT_SEQUENCE_CONSEQUENCE_TYPE);
        singleValueDistinctFieldColumns.add(GENE_ALLELE_VARIANT_SEQUENCE_VARIANT_POLYPHEN);
        singleValueDistinctFieldColumns.add(GENE_ALLELE_VARIANT_SEQUENCE_VARIANT_SIFT);
    }

}
