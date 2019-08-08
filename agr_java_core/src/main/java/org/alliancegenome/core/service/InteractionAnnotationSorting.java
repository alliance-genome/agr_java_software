package org.alliancegenome.core.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.alliancegenome.neo4j.entity.Sorting;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;
import org.alliancegenome.neo4j.entity.node.MITerm;

public class InteractionAnnotationSorting implements Sorting<InteractionGeneJoin> {

    private List<Comparator<InteractionGeneJoin>> defaultList;
    private List<Comparator<InteractionGeneJoin>> moleculeList;
    private List<Comparator<InteractionGeneJoin>> detectionList;
    private List<Comparator<InteractionGeneJoin>> interactorMoleculeTypeList;
    private List<Comparator<InteractionGeneJoin>> interactorSpeciesList;
    private List<Comparator<InteractionGeneJoin>> referenceList;

    private static Comparator<InteractionGeneJoin> interactorGeneSymbolOrder =
            Comparator.comparing(annotation -> Sorting.getSmartKey(annotation.getGeneB().getSymbol()));

    private static Comparator<InteractionGeneJoin> moleculeOrder =
            Comparator.comparing(annotation -> annotation.getInteractorAType().getLabel().toLowerCase());

    private static Comparator<InteractionGeneJoin> interactorMoleculeOrder =
            Comparator.comparing(annotation -> annotation.getInteractorBType().getLabel().toLowerCase());

    private static Comparator<InteractionGeneJoin> interactorSpeciesOrder =
            Comparator.comparing(annotation -> annotation.getGeneB().getSpecies().getName().toLowerCase());

    private static Comparator<InteractionGeneJoin> referenceOrder =
            Comparator.comparing(annotation -> annotation.getPublication().getPubId().toLowerCase());

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

        interactorMoleculeTypeList = new ArrayList<>(4);
        interactorMoleculeTypeList.add(interactorMoleculeOrder);
        interactorMoleculeTypeList.add(moleculeOrder);
        interactorMoleculeTypeList.add(interactorGeneSymbolOrder);
        interactorMoleculeTypeList.add(interactorSpeciesOrder);

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

        referenceList = new ArrayList<>(4);
        referenceList.add(referenceOrder);
        referenceList.add(interactorGeneSymbolOrder);
        referenceList.add(moleculeOrder);
        referenceList.add(interactorMoleculeOrder);
    }

    public Comparator<InteractionGeneJoin> getComparator(SortingField field, Boolean ascending) {
        if (field == null)
            return getJoinedComparator(defaultList);

        switch (field) {
            case INTERACTOR_GENE_SYMBOL:
                return getJoinedComparator(defaultList);
            case MOLECULE_TYPE:
                return getJoinedComparator(moleculeList);
            case INTERACTOR_MOLECULE_TYPE:
                return getJoinedComparator(interactorMoleculeTypeList);
            case INTERACTOR_SPECIES:
                return getJoinedComparator(interactorSpeciesList);
            case INTERACTOR_DETECTION_METHOD:
                return getJoinedComparator(detectionList);
            case REFERENCE:
                return getJoinedComparator(referenceList);
            default:
                return getJoinedComparator(defaultList);
        }
    }


}
