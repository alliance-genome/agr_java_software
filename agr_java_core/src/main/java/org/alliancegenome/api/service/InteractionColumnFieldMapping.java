package org.alliancegenome.api.service;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import static org.alliancegenome.api.service.Column.*;

public class InteractionColumnFieldMapping extends ColumnFieldMapping<InteractionGeneJoin> {

    private Map<Column, Function<InteractionGeneJoin, String>> mapColumnAttribute = new HashMap<>();

    public Map<Column, Function<InteractionGeneJoin, String>> getMapColumnAttribute() {
        return mapColumnAttribute;
    }

    public InteractionColumnFieldMapping() {
        mapColumnFieldName.put(INTERACTOR_MOLECULE_TYPE, FieldFilter.INTERACTOR_MOLECULE_TYPE);
        mapColumnFieldName.put(MOLECULE_TYPE, FieldFilter.MOLECULE_TYPE);
        mapColumnFieldName.put(INTERACTOR_SPECIES, FieldFilter.INTERACTOR_SPECIES);

        mapColumnAttribute.put(INTERACTOR_MOLECULE_TYPE, (join) -> join.getInteractorBType().getDisplayName());
        mapColumnAttribute.put(MOLECULE_TYPE, (join) -> join.getInteractorAType().getDisplayName());
        mapColumnAttribute.put(INTERACTOR_SPECIES, (join) -> join.getGeneB().getSpecies().getName());

        singleValueDistinctFieldColumns.add(INTERACTOR_MOLECULE_TYPE);
        singleValueDistinctFieldColumns.add(MOLECULE_TYPE);
        singleValueDistinctFieldColumns.add(INTERACTOR_SPECIES);
    }

}
