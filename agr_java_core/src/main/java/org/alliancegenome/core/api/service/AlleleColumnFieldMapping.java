package org.alliancegenome.core.api.service;

import static java.util.stream.Collectors.toSet;
import static org.alliancegenome.core.api.service.Column.GENE_ALLELE_CATEGORY;
import static org.alliancegenome.core.api.service.Column.GENE_ALLELE_HAS_DISEASE;
import static org.alliancegenome.core.api.service.Column.GENE_ALLELE_HAS_PHENOTYPE;
import static org.alliancegenome.core.api.service.Column.GENE_ALLELE_VARIANT_CONSEQUENCE;
import static org.alliancegenome.core.api.service.Column.GENE_ALLELE_VARIANT_TYPE;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.apache.commons.collections4.CollectionUtils;

public class AlleleColumnFieldMapping extends ColumnFieldMapping<Allele> {

	private Map<Column, Function<Allele, Set<String>>> mapColumnAttribute = new HashMap<>();

	public Map<Column, Function<Allele, Set<String>>> getMapColumnAttribute() {
		return mapColumnAttribute;
	}

	public AlleleColumnFieldMapping() {
		mapColumnFieldName.put(GENE_ALLELE_VARIANT_TYPE, FieldFilter.VARIANT_TYPE);
		mapColumnFieldName.put(GENE_ALLELE_VARIANT_CONSEQUENCE, FieldFilter.MOLECULAR_CONSEQUENCE);
		mapColumnFieldName.put(GENE_ALLELE_CATEGORY, FieldFilter.ALLELE_CATEGORY);
		mapColumnFieldName.put(GENE_ALLELE_HAS_DISEASE, FieldFilter.HAS_DISEASE);
		mapColumnFieldName.put(GENE_ALLELE_HAS_PHENOTYPE, FieldFilter.HAS_PHENOTYPE);

		mapColumnAttribute.put(GENE_ALLELE_CATEGORY, entity -> Set.of(entity.getCategory()));
		mapColumnAttribute.put(GENE_ALLELE_VARIANT_TYPE, entity -> {
			if (entity.getVariants() != null) {
				return entity.getVariants().stream()
						.filter(variant -> variant.getVariantType() != null)
						.map(variant -> variant.getVariantType().getName()).collect(toSet());
			}
			return new HashSet<>();
		});
		mapColumnAttribute.put(GENE_ALLELE_VARIANT_CONSEQUENCE, entity -> {
			if (CollectionUtils.isNotEmpty(entity.getVariants())) {
				Set<String> ret = entity.getVariants().stream()
						.map(v->v.getTranscriptLevelConsequence())
						.filter(Objects::nonNull)
						.flatMap(Collection::stream)
						.map(tlc -> tlc.getMolecularConsequences())
						.filter(Objects::nonNull)
						.flatMap(List::stream)
						.collect(Collectors.toSet());
				if(ret == null) return new HashSet<>();
				return ret;

			}
			return new HashSet<>();
		});

		mapColumnAttribute.put(GENE_ALLELE_HAS_DISEASE, entity -> entity.hasDisease() ? Set.of("YES") : Set.of("NO"));
		mapColumnAttribute.put(GENE_ALLELE_HAS_PHENOTYPE, entity -> entity.hasPhenotype() ? Set.of("YES") : Set.of("NO"));

		singleValueDistinctFieldColumns.add(GENE_ALLELE_VARIANT_TYPE);
		singleValueDistinctFieldColumns.add(GENE_ALLELE_VARIANT_CONSEQUENCE);
		singleValueDistinctFieldColumns.add(GENE_ALLELE_CATEGORY);
		singleValueDistinctFieldColumns.add(GENE_ALLELE_HAS_DISEASE);
		singleValueDistinctFieldColumns.add(GENE_ALLELE_HAS_PHENOTYPE);
	}

}
