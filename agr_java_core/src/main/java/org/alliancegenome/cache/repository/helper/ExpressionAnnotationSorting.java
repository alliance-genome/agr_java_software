package org.alliancegenome.cache.repository.helper;

import java.util.*;

import org.alliancegenome.core.ExpressionDetail;
import org.alliancegenome.neo4j.entity.Sorting;

public class ExpressionAnnotationSorting implements Sorting<ExpressionDetail> {

    private List<Comparator<ExpressionDetail>> defaultList;
    private List<Comparator<ExpressionDetail>> locationList;
    private List<Comparator<ExpressionDetail>> assayList;
    private List<Comparator<ExpressionDetail>> stageList;
    private List<Comparator<ExpressionDetail>> geneList;

    private static Comparator<ExpressionDetail> termNameOrder =
            Comparator.comparing(annotation -> annotation.getTermName().toLowerCase());

    private static Comparator<ExpressionDetail> stageOrder =
            Comparator.comparing(annotation -> annotation.getStage().getName().toLowerCase());

    private static Comparator<ExpressionDetail> assayOrder =
            Comparator.comparing(annotation -> annotation.getAssay().getDisplaySynonym().toLowerCase());

    private static Comparator<ExpressionDetail> speciesOrder =
            Comparator.comparing(annotation -> annotation.getGene().getSpecies().getName().toLowerCase());

    private static Comparator<ExpressionDetail> geneOrder =
            Comparator.comparing(annotation -> annotation.getGene().getSymbol().toLowerCase());

    public ExpressionAnnotationSorting() {
        super();

        defaultList = new ArrayList<>(5);
        defaultList.add(speciesOrder);
        defaultList.add(geneOrder);
        defaultList.add(termNameOrder);
        defaultList.add(stageOrder);
        defaultList.add(assayOrder);

        locationList = new ArrayList<>(5);
        locationList.add(termNameOrder);
        locationList.add(speciesOrder);
        locationList.add(geneOrder);
        locationList.add(stageOrder);
        locationList.add(assayOrder);

        assayList = new ArrayList<>(5);
        assayList.add(assayOrder);
        assayList.add(speciesOrder);
        assayList.add(geneOrder);
        assayList.add(termNameOrder);
        assayList.add(stageOrder);

        stageList = new ArrayList<>(5);
        stageList.add(speciesOrder);
        stageList.add(stageOrder);
        stageList.add(geneOrder);
        stageList.add(termNameOrder);
        stageList.add(assayOrder);

        geneList = new ArrayList<>(5);
        geneList.add(geneOrder);
        geneList.add(speciesOrder);
        geneList.add(termNameOrder);
        geneList.add(stageOrder);
        geneList.add(assayOrder);
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
            case GENE:
                return getJoinedComparator(geneList);
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
