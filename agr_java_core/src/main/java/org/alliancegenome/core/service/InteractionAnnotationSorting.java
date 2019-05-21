package org.alliancegenome.core.service;

import org.alliancegenome.neo4j.entity.Sorting;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class InteractionAnnotationSorting implements Sorting<InteractionGeneJoin> {


    public Comparator<InteractionGeneJoin> getComparator(SortingField field, Boolean ascending) {
        if (field == null)
            return getJoinedComparator(defaultList);

        switch (field) {
            case INTERACTOR_GENE_SYMBOL:
                return getJoinedComparator(defaultList);
            case INTERACTOR_MOLECULE_TYPE:
                return getJoinedComparator(moleculeList);
            case INTERACTOR_SPECIES:
                return getJoinedComparator(interactorSpeciesList);
            default:
                return getJoinedComparator(defaultList);
        }
    }

    private static Comparator<InteractionGeneJoin> interactorGeneSymbolOrder =
            Comparator.comparing(annotation -> annotation.getGeneB().getSymbol().toLowerCase());

    private static Comparator<InteractionGeneJoin> moleculeOrder =
            Comparator.comparing(annotation -> annotation.getInteractorAType().getLabel().toLowerCase());

    private static Comparator<InteractionGeneJoin> interactorMoleculeOrder =
            Comparator.comparing(annotation -> annotation.getInteractorBType().getLabel().toLowerCase());

    private static Comparator<InteractionGeneJoin> interactorSpeciesOrder =
            Comparator.comparing(annotation -> annotation.getGeneB().getSpecies().getName().toLowerCase());

    private static List<Comparator<InteractionGeneJoin>> defaultList = new ArrayList<>();

    static {
        defaultList.add(interactorGeneSymbolOrder);
        defaultList.add(moleculeOrder);
        defaultList.add(interactorMoleculeOrder);
        defaultList.add(interactorSpeciesOrder);
    }

    private static List<Comparator<InteractionGeneJoin>> moleculeList = new ArrayList<>();

    static {
        moleculeList.add(moleculeOrder);
        moleculeList.add(interactorMoleculeOrder);
        moleculeList.add(interactorGeneSymbolOrder);
        moleculeList.add(interactorSpeciesOrder);
    }

    private static List<Comparator<InteractionGeneJoin>> detectionList = new ArrayList<>();

    static {
        detectionList.add(moleculeOrder);
        detectionList.add(interactorMoleculeOrder);
        detectionList.add(interactorGeneSymbolOrder);
        detectionList.add(interactorSpeciesOrder);
    }

    private static List<Comparator<InteractionGeneJoin>> interactorSpeciesList = new ArrayList<>();

    static {
        interactorSpeciesList.add(interactorSpeciesOrder);
        interactorSpeciesList.add(interactorGeneSymbolOrder);
        interactorSpeciesList.add(moleculeOrder);
        interactorSpeciesList.add(interactorMoleculeOrder);
    }


}
