package org.alliancegenome.api.service;

import static org.alliancegenome.api.service.Column.INTERACTOR_DETECTION_METHOD;
import static org.alliancegenome.api.service.Column.INTERACTOR_GENE_SYMBOL;
import static org.alliancegenome.api.service.Column.INTERACTOR_MOLECULE_TYPE;
import static org.alliancegenome.api.service.Column.INTERACTOR_REFERENCE;
import static org.alliancegenome.api.service.Column.INTERACTOR_SOURCE;
import static org.alliancegenome.api.service.Column.INTERACTOR_SPECIES;
import static org.alliancegenome.api.service.Column.MOLECULE_TYPE;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;

public class InteractionColumnFieldMapping extends ColumnFieldMapping<InteractionGeneJoin> {

    private Map<Column, Function<InteractionGeneJoin, String>> mapColumnAttribute = new HashMap<>();

    public Map<Column, Function<InteractionGeneJoin, String>> getMapColumnAttribute() {
        return mapColumnAttribute;
    }

    public InteractionColumnFieldMapping() {
        mapColumnFieldName.put(INTERACTOR_MOLECULE_TYPE, FieldFilter.INTERACTOR_MOLECULE_TYPE);
        mapColumnFieldName.put(MOLECULE_TYPE, FieldFilter.MOLECULE_TYPE);
        mapColumnFieldName.put(INTERACTOR_SPECIES, FieldFilter.INTERACTOR_SPECIES);
        mapColumnFieldName.put(INTERACTOR_GENE_SYMBOL, FieldFilter.INTERACTOR_GENE_SYMBOL);
        mapColumnFieldName.put(INTERACTOR_SOURCE, FieldFilter.SOURCE);
        mapColumnFieldName.put(INTERACTOR_REFERENCE, FieldFilter.FREFERENCE);
        mapColumnFieldName.put(INTERACTOR_DETECTION_METHOD, FieldFilter.DETECTION_METHOD);

        mapColumnAttribute.put(INTERACTOR_MOLECULE_TYPE, (join) -> join.getInteractorBType().getDisplayName());
        mapColumnAttribute.put(MOLECULE_TYPE, (join) -> join.getInteractorAType().getDisplayName());
        mapColumnAttribute.put(INTERACTOR_SPECIES, (join) -> join.getGeneB().getSpecies().getName());

        singleValueDistinctFieldColumns.add(INTERACTOR_MOLECULE_TYPE);
        singleValueDistinctFieldColumns.add(MOLECULE_TYPE);
        singleValueDistinctFieldColumns.add(INTERACTOR_SPECIES);
    }

}
