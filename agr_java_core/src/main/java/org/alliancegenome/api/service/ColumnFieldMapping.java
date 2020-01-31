package org.alliancegenome.api.service;

import org.alliancegenome.es.model.query.FieldFilter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public abstract class ColumnFieldMapping<T> {

    Map<Column, FieldFilter> mapColumnFieldName = new HashMap<>();

/*
    static {
        mapColumnFieldName.put(Column.MOLECULE_TYPE, FieldFilter.MOLECULE_TYPE);
        mapColumnFieldName.put(Column.INTERACTOR_GENE, FieldFilter.INTERACTOR_GENE_SYMBOL);
        mapColumnFieldName.put(Column.INTERACTOR_SPECIES, FieldFilter.INTERACTOR_SPECIES);
        mapColumnFieldName.put(Column.INTERACTOR_MOLECULE_TYPE, FieldFilter.INTERACTOR_MOLECULE_TYPE);
        mapColumnFieldName.put(Column.INTERACTION_DETECTION_TYPE, FieldFilter.DETECTION_METHOD);
        mapColumnFieldName.put(Column.INERACTION_SOURCE, FieldFilter.SOURCE);
        mapColumnFieldName.put(Column.REFERENCE, FieldFilter.FREFERENCE);

        mapColumnFieldName.put(ASSOCIATES_GENES_SPECIES, FieldFilter.SPECIES);
        mapColumnFieldName.put(Column.ASSOCIATES_GENES_ASSOCIATION, FieldFilter.ASSOCIATION_TYPE);

    }
*/



/*
    static {
        mapColumnAttribute.put(Column.MOLECULE_TYPE, (InteractionGeneJoin entity) -> entity.getInteractorAType().getDisplayName());
        mapColumnAttribute.put(Column.INTERACTOR_GENE, (InteractionGeneJoin entity) -> entity.getGeneB().getSymbol());
        mapColumnAttribute.put(Column.INTERACTOR_SPECIES, (InteractionGeneJoin entity) -> entity.getGeneB().getSpecies().getName());
        mapColumnAttribute.put(Column.INTERACTOR_MOLECULE_TYPE, (InteractionGeneJoin entity) -> entity.getInteractorBType().getDisplayName());
        mapColumnAttribute.put(Column.REFERENCE, (InteractionGeneJoin entity) -> entity.getPublication().getPubId());

        mapColumnAttribute.put(ASSOCIATES_GENES_SPECIES, (DiseaseAnnotation entity) -> entity.getGene().getSpecies().getName());
        mapColumnAttribute.put(ASSOCIATES_GENES_ASSOCIATION, (Function<DiseaseAnnotation, String>) DiseaseAnnotation::getAssociationType);
    }
*/

    List<Column> singleValueDistinctFieldColumns = new ArrayList<>();

    public abstract Map<Column, Function<T, String>> getMapColumnAttribute();

    public Map<Column, Function<T, String>> getSingleValuedFieldColumns(Table table) {
        Map<Column, Function<T, String>> map = new HashMap<>();
        singleValueDistinctFieldColumns.stream()
                .filter(column -> column.getTable().equals(table))
                .forEach(column -> {
                    getMapColumnAttribute().entrySet().stream()
                            .filter(entry -> entry.getKey().equals(column))
                            .forEach(entry -> map.put(entry.getKey(), entry.getValue()));
                });
        return map;
    }

    public String getFieldFilterName(Column column) {
        return mapColumnFieldName.get(column).getName();
    }
}
