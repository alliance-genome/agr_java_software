package org.alliancegenome.core.service;

import org.alliancegenome.neo4j.entity.Sorting;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;

import java.util.*;

public class InteractionAnnotationSorting implements Sorting<InteractionGeneJoin> {


    public Comparator<InteractionGeneJoin> getComparator(SortingField field, Boolean ascending) {
        if (field == null)
            return getDefaultComparator();

        List<Comparator<InteractionGeneJoin>> comparatorList = new ArrayList<>();
        switch (field) {
            case INTERACTOR_GENE_SYMBOL:
                getDefaultComparator();
                break;
            default:
                break;
        }
        return getJoinedComparator(comparatorList);
    }

    public Comparator<InteractionGeneJoin> getDefaultComparator() {
        List<Comparator<InteractionGeneJoin>> comparatorList = new ArrayList<>();
        comparatorList.add(interactorGeneSymbolOrder);
        comparatorList.add(moleculeOrder);
        comparatorList.add(interactorMoleculeOrder);
        comparatorList.add(interactorSpeciesOrder);
        return getJoinedComparator(comparatorList);
    }

    private static Comparator<InteractionGeneJoin> interactorGeneSymbolOrder =
            Comparator.comparing(annotation -> annotation.getGeneB().getSymbol().toLowerCase());

    private static Comparator<InteractionGeneJoin> moleculeOrder =
            Comparator.comparing(annotation -> annotation.getInteractorAType().getLabel().toLowerCase());

    private static Comparator<InteractionGeneJoin> interactorMoleculeOrder =
            Comparator.comparing(annotation -> annotation.getInteractorBType().getLabel().toLowerCase());

    private static Comparator<InteractionGeneJoin> interactorSpeciesOrder =
            Comparator.comparing(annotation -> annotation.getGeneB().getSpecies().getName().toLowerCase());

    private static Map<SortingField, Comparator<InteractionGeneJoin>> sortingFieldMap = new LinkedHashMap<>();

    static {
        sortingFieldMap.put(SortingField.INTERACTOR_GENE_SYMBOL, interactorGeneSymbolOrder);
    }

}
