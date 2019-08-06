package org.alliancegenome.core.service;

import org.alliancegenome.core.ExpressionDetail;
import org.alliancegenome.neo4j.entity.Sorting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ExpressionAnnotationSorting implements Sorting<ExpressionDetail> {

    private List<Comparator<ExpressionDetail>> defaultList;
    private List<Comparator<ExpressionDetail>> speciesList;
    private List<Comparator<ExpressionDetail>> locationList;
    private List<Comparator<ExpressionDetail>> assayList;
    private List<Comparator<ExpressionDetail>> stageList;

    private static Comparator<ExpressionDetail> termNameOrder =
            Comparator.comparing(annotation -> annotation.getTermName().toLowerCase());

    private static Comparator<ExpressionDetail> stageOrder =
            Comparator.comparing(annotation -> annotation.getStage().getName().toLowerCase());

    private static Comparator<ExpressionDetail> assayOrder =
            Comparator.comparing(annotation -> annotation.getAssay().getName().toLowerCase());

    private static Comparator<ExpressionDetail> speciesOrder =
            Comparator.comparing(annotation -> annotation.getGene().getSpecies().getName().toLowerCase());

    public ExpressionAnnotationSorting() {
        super();

        defaultList = new ArrayList<>(4);
        defaultList.add(speciesOrder);
        defaultList.add(termNameOrder);
        defaultList.add(stageOrder);
        defaultList.add(assayOrder);

        speciesList = new ArrayList<>(4);
/*
        speciesList.add(moleculeOrder);
        speciesList.add(interactorMoleculeOrder);
        speciesList.add(interactorGeneSymbolOrder);
        speciesList.add(interactorSpeciesOrder);
*/

        locationList = new ArrayList<>(4);
        locationList.add(termNameOrder);
        locationList.add(speciesOrder);
        locationList.add(stageOrder);
        locationList.add(assayOrder);

        assayList = new ArrayList<>(4);
        assayList.add(assayOrder);
        assayList.add(speciesOrder);
        assayList.add(termNameOrder);
        assayList.add(stageOrder);

        stageList = new ArrayList<>(4);
        stageList.add(speciesOrder);
        stageList.add(stageOrder);
        stageList.add(termNameOrder);
        stageList.add(assayOrder);
    }

    public Comparator<ExpressionDetail> getComparator(SortingField field, Boolean ascending) {
        if (field == null)
            return getJoinedComparator(defaultList);

        switch (field) {
            case DEFAULT:
                return getJoinedComparator(defaultList);
            case SPECIES:
                return getJoinedComparator(defaultList);
            case LOCATION:
                return getJoinedComparator(locationList);
            case ASSAY:
                return getJoinedComparator(assayList);
            case STAGE:
                return getJoinedComparator(stageList);
            default:
                return getJoinedComparator(defaultList);
        }
    }

    public Comparator<ExpressionDetail> getDefaultComparator() {
        List<Comparator<ExpressionDetail>> comparatorList = new ArrayList<>();
        comparatorList.add(termNameOrder);
        comparatorList.add(stageOrder);
        comparatorList.add(assayOrder);
        return getJoinedComparator(comparatorList);
    }

}
