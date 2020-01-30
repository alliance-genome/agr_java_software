package org.alliancegenome.neo4j.entity;

import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;

import java.util.function.Function;

public class InteractionFieldValues extends FieldValues<InteractionGeneJoin> {

    public Function<InteractionGeneJoin, String> interactorAType =
            (join) -> join.getInteractorAType().getDisplayName();

    public Function<InteractionGeneJoin, String> interactorBType =
            (join) -> join.getInteractorBType().getDisplayName();

    public Function<InteractionGeneJoin, String> interactorSpecies =
            (join) -> join.getGeneB().getSpecies().getName();

    public InteractionFieldValues() {
        filterFields.put(FieldFilter.MOLECULE_TYPE, interactorAType);
        filterFields.put(FieldFilter.INTERACTOR_MOLECULE_TYPE, interactorBType);
        filterFields.put(FieldFilter.INTERACTOR_SPECIES, interactorSpecies);
    }
}
