package org.alliancegenome.api.service;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.es.model.query.FieldFilter;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.alliancegenome.api.service.Column.*;

@Log4j2
public class AlleleVariantSequenceColumnFieldMapping extends ColumnFieldMapping<AlleleVariantSequence> {

    private Map<Column, Function<AlleleVariantSequence, Set<String>>> mapColumnAttribute = new HashMap<>();

    public Map<Column, Function<AlleleVariantSequence, Set<String>>> getMapColumnAttribute() {
        return mapColumnAttribute;
    }

    public AlleleVariantSequenceColumnFieldMapping() {
        mapColumnFieldName.put(GENE_ALLELE_VARIANT_SEQUENCE_VARIANT_TYPE, FieldFilter.VARIANT_TYPE);
        mapColumnFieldName.put(GENE_ALLELE_VARIANT_SEQUENCE_TYPE, FieldFilter.SEQUENCE_FEATURE_TYPE);
        mapColumnFieldName.put(GENE_ALLELE_VARIANT_SEQUENCE_CONSEQUENCE, FieldFilter.MOLECULAR_CONSEQUENCE);
        mapColumnFieldName.put(GENE_ALLELE_VARIANT_SEQUENCE_CATEGORY, FieldFilter.ALLELE_CATEGORY);
        mapColumnFieldName.put(GENE_ALLELE_VARIANT_SEQUENCE_IMPACT, FieldFilter.VARIANT_IMPACT);
        mapColumnFieldName.put(GENE_ALLELE_VARIANT_SEQUENCE_VARIANT_POLYPHEN, FieldFilter.VARIANT_POLYPHEN);
        mapColumnFieldName.put(GENE_ALLELE_VARIANT_SEQUENCE_VARIANT_SIFT, FieldFilter.VARIANT_SIFT);
        mapColumnFieldName.put(GENE_ALLELE_VARIANT_SEQUENCE_HAS_PHENOTYPE, FieldFilter.HAS_PHENOTYPE);
        mapColumnFieldName.put(GENE_ALLELE_VARIANT_SEQUENCE_HAS_DISEASE, FieldFilter.HAS_DISEASE);
        mapColumnFieldName.put(GENE_ALLELE_VARIANT_SEQUENCE_ASSOCIATED_GENE, FieldFilter.ASSOCIATED_GENE);

        mapColumnAttribute.put(GENE_ALLELE_VARIANT_SEQUENCE_CATEGORY, entity -> Set.of(entity.getAllele().getCategory()));

        mapColumnAttribute.put(GENE_ALLELE_VARIANT_SEQUENCE_TYPE, entity -> {
            if (entity.getConsequence() != null) {
                if (StringUtils.isNotEmpty(entity.getConsequence().getSequenceFeatureType())) {
                    return Set.of(entity.getConsequence().getSequenceFeatureType());
                }
                log.error("Could not find sequence feature type for "+entity.getVariant().getHgvsNomenclature());
            }
            return Set.of("");
        });
        mapColumnAttribute.put(GENE_ALLELE_VARIANT_SEQUENCE_ASSOCIATED_GENE, entity -> {
            if (entity.getAllele().getGene() != null) {
                return Set.of(entity.getAllele().getGene().getSymbol());
            }
            return new HashSet<>();
        });
        mapColumnAttribute.put(GENE_ALLELE_VARIANT_SEQUENCE_VARIANT_TYPE, entity -> {
            if (entity.getVariant() != null && entity.getVariant().getVariantType() != null) {
                return Set.of(entity.getVariant().getVariantType().getName());
            }
            return Set.of("");
        });
        mapColumnAttribute.put(GENE_ALLELE_VARIANT_SEQUENCE_CONSEQUENCE, entity -> {
            if (entity.getVariant() != null) {
                if (entity.getConsequence() != null)
                    return Set.copyOf(entity.getConsequence().getTranscriptLevelConsequences());
            }
            return new HashSet<>();
        });
        mapColumnAttribute.put(GENE_ALLELE_VARIANT_SEQUENCE_IMPACT, entity -> {
            if (entity.getVariant() != null) {
                if (entity.getConsequence() != null)
                    return Set.of(entity.getConsequence().getImpact());
            }
            return Set.of("");
        });

        mapColumnAttribute.put(GENE_ALLELE_VARIANT_SEQUENCE_VARIANT_POLYPHEN, entity -> {
            if (entity.getVariant() != null) {
                if (entity.getConsequence() != null && entity.getConsequence().getPolyphenPrediction() != null)
                    return Set.of(entity.getConsequence().getPolyphenPrediction());
            }
            return Set.of("");
        });

        mapColumnAttribute.put(GENE_ALLELE_VARIANT_SEQUENCE_VARIANT_SIFT, entity -> {
            if (entity.getVariant() != null) {
                if (entity.getConsequence() != null && entity.getConsequence().getSiftPrediction() != null)
                    return Set.of(entity.getConsequence().getSiftPrediction());
            }
            return Set.of("");
        });

        mapColumnAttribute.put(GENE_ALLELE_VARIANT_SEQUENCE_HAS_DISEASE, entity -> entity.getAllele().hasDisease() ? Set.of("YES") : Set.of("NO"));
        mapColumnAttribute.put(GENE_ALLELE_VARIANT_SEQUENCE_HAS_PHENOTYPE, entity -> entity.getAllele().hasPhenotype() ? Set.of("YES") : Set.of("NO"));


        singleValueDistinctFieldColumns.add(GENE_ALLELE_VARIANT_SEQUENCE_VARIANT_TYPE);
        singleValueDistinctFieldColumns.add(GENE_ALLELE_VARIANT_SEQUENCE_TYPE);
        singleValueDistinctFieldColumns.add(GENE_ALLELE_VARIANT_SEQUENCE_CONSEQUENCE);
        singleValueDistinctFieldColumns.add(GENE_ALLELE_VARIANT_SEQUENCE_CATEGORY);
        singleValueDistinctFieldColumns.add(GENE_ALLELE_VARIANT_SEQUENCE_IMPACT);
        singleValueDistinctFieldColumns.add(GENE_ALLELE_VARIANT_SEQUENCE_VARIANT_POLYPHEN);
        singleValueDistinctFieldColumns.add(GENE_ALLELE_VARIANT_SEQUENCE_VARIANT_SIFT);
        singleValueDistinctFieldColumns.add(GENE_ALLELE_VARIANT_SEQUENCE_HAS_DISEASE);
        singleValueDistinctFieldColumns.add(GENE_ALLELE_VARIANT_SEQUENCE_HAS_PHENOTYPE);
        singleValueDistinctFieldColumns.add(GENE_ALLELE_VARIANT_SEQUENCE_ASSOCIATED_GENE);
    }

}
