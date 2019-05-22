package org.alliancegenome.core.service;

import org.alliancegenome.neo4j.entity.Sorting;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.entity.node.MITerm;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class InteractionAnnotationSorting implements Sorting<InteractionGeneJoin> {

    private List<Comparator<InteractionGeneJoin>> defaultList;
    private List<Comparator<InteractionGeneJoin>> moleculeList;
    private List<Comparator<InteractionGeneJoin>> detectionList;
    private List<Comparator<InteractionGeneJoin>> interactorSpeciesList;

    private static Comparator<InteractionGeneJoin> interactorGeneSymbolOrder =
            Comparator.comparing(annotation -> annotation.getGeneB().getSymbol().toLowerCase());

    private static Comparator<InteractionGeneJoin> moleculeOrder =
            Comparator.comparing(annotation -> annotation.getInteractorAType().getLabel().toLowerCase());

    private static Comparator<InteractionGeneJoin> interactorMoleculeOrder =
            Comparator.comparing(annotation -> annotation.getInteractorBType().getLabel().toLowerCase());

    private static Comparator<InteractionGeneJoin> interactorSpeciesOrder =
            Comparator.comparing(annotation -> annotation.getGeneB().getSpecies().getName().toLowerCase());

    private static Comparator<InteractionGeneJoin> detectionOrder =
            Comparator.comparing(annotation -> {
                List<MITerm> terms = annotation.getDetectionsMethods();
                terms.sort(Comparator.comparing(miTerm -> miTerm.getDisplayName().toLowerCase()));
                return terms.get(0).getDisplayName().toLowerCase();
            });


    public InteractionAnnotationSorting() {
        super();

        defaultList = new ArrayList<>(4);
        defaultList.add(interactorGeneSymbolOrder);
        defaultList.add(moleculeOrder);
        defaultList.add(interactorMoleculeOrder);
        defaultList.add(interactorSpeciesOrder);

        moleculeList = new ArrayList<>(4);
        moleculeList.add(moleculeOrder);
        moleculeList.add(interactorMoleculeOrder);
        moleculeList.add(interactorGeneSymbolOrder);
        moleculeList.add(interactorSpeciesOrder);

        detectionList = new ArrayList<>(4);
        detectionList.add(detectionOrder);
        detectionList.add(interactorGeneSymbolOrder);
        detectionList.add(moleculeOrder);
        detectionList.add(interactorMoleculeOrder);

        interactorSpeciesList = new ArrayList<>(4);
        interactorSpeciesList.add(interactorSpeciesOrder);
        interactorSpeciesList.add(interactorGeneSymbolOrder);
        interactorSpeciesList.add(moleculeOrder);
        interactorSpeciesList.add(interactorMoleculeOrder);
    }

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
            case INTERACTOR_DETECTION_METHOD:
                return getJoinedComparator(detectionList);
            default:
                return getJoinedComparator(defaultList);
        }
    }


}
