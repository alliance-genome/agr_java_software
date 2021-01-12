package org.alliancegenome.api.service;

import static org.alliancegenome.api.service.Column.TG_ALLELE_SPECIES;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.neo4j.entity.node.Allele;

public class TransgenicAlleleColumnFieldMapping extends ColumnFieldMapping<Allele> {

    private Map<Column, Function<Allele, Set<String>>> mapColumnAttribute = new HashMap<>();

    public Map<Column, Function<Allele, Set<String>>> getMapColumnAttribute() {
        return mapColumnAttribute;
    }

    public TransgenicAlleleColumnFieldMapping() {
        mapColumnFieldName.put(TG_ALLELE_SPECIES, FieldFilter.SPECIES);

        mapColumnAttribute.put(TG_ALLELE_SPECIES, entity -> {
            if (entity.getSpecies() != null) {
                return Set.of(entity.getSpecies().getType().getName());
            }
            return null;
        });

        singleValueDistinctFieldColumns.add(TG_ALLELE_SPECIES);
    }

}
