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

        mapColumnAttribute.put(GENE_ALLELE_VARIANT_SEQUENCE_CATEGORY, entity -> Set.of(entity.getAllele().getCategory()));
        mapColumnAttribute.put(GENE_ALLELE_VARIANT_TYPE, entity -> {
            if (entity.getVariant() != null) {
                return Set.of(entity.getVariant().getVariantType().getName());
            }
            return null;
        });
        mapColumnAttribute.put(GENE_ALLELE_VARIANT_CONSEQUENCE, entity -> {
            if (entity.getVariant() != null) {
                return Set.of(entity.getConsequence().getTranscriptLevelConsequence());
            }
            return new HashSet<>();
        });

        singleValueDistinctFieldColumns.add(GENE_ALLELE_VARIANT_SEQUENCE_TYPE);
        singleValueDistinctFieldColumns.add(GENE_ALLELE_VARIANT_SEQUENCE_CONSEQUENCE);
        singleValueDistinctFieldColumns.add(GENE_ALLELE_VARIANT_SEQUENCE_CATEGORY);
    }

}
