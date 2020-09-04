package org.alliancegenome.api.service;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.apache.commons.collections4.CollectionUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static java.util.stream.Collectors.toSet;
import static org.alliancegenome.api.service.Column.GENE_ALLELE_VARIANT_CONSEQUENCE;
import static org.alliancegenome.api.service.Column.GENE_ALLELE_VARIANT_TYPE;

public class AlleleColumnFieldMapping extends ColumnFieldMapping<Allele> {

    private Map<Column, Function<Allele, Set<String>>> mapColumnAttribute = new HashMap<>();

    public Map<Column, Function<Allele, Set<String>>> getMapColumnAttribute() {
        return mapColumnAttribute;
    }

    public AlleleColumnFieldMapping() {
        mapColumnFieldName.put(GENE_ALLELE_VARIANT_TYPE, FieldFilter.VARIANT_TYPE);
        mapColumnFieldName.put(GENE_ALLELE_VARIANT_CONSEQUENCE, FieldFilter.VARIANT_CONSEQUENCE);

        mapColumnAttribute.put(GENE_ALLELE_VARIANT_TYPE, entity -> {
            if (entity.getVariants() != null) {
                return entity.getVariants().stream().map(variant -> variant.getVariationType().getName()).collect(toSet());
            }
            return null;
        });
        mapColumnAttribute.put(GENE_ALLELE_VARIANT_CONSEQUENCE, entity -> {
            if (CollectionUtils.isNotEmpty(entity.getVariants())) {
                return entity.getVariants().stream()
                        .filter(variant -> variant.getGeneLevelConsequence() != null)
                        .map(variant -> variant.getGeneLevelConsequence().getGeneLevelConsequence())
                        .collect(toSet());
            }
            return new HashSet<>();
        });

        singleValueDistinctFieldColumns.add(GENE_ALLELE_VARIANT_TYPE);
        singleValueDistinctFieldColumns.add(GENE_ALLELE_VARIANT_CONSEQUENCE);
    }

}
