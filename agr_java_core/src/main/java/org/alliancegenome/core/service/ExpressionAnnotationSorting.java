package org.alliancegenome.core.service;

import org.alliancegenome.core.ExpressionDetail;
import org.alliancegenome.neo4j.entity.Sorting;

import java.util.*;

public class ExpressionAnnotationSorting implements Sorting<ExpressionDetail> {


    public Comparator<ExpressionDetail> getComparator(SortingField field, Boolean ascending) {
        if (field == null)
            return getDefaultComparator();

        List<Comparator<ExpressionDetail>> comparatorList = new ArrayList<>();
        switch (field) {
            case STAGE:
                if (!ascending)
                    comparatorList.add(stageOrder.reversed());
                else
                    comparatorList.add(stageOrder);
                break;
            case ASSAY:
                if (!ascending)
                    comparatorList.add(assayOrder.reversed());
                else
                    comparatorList.add(assayOrder);
                break;
            case EXPRESSION:
                if (!ascending)
                    comparatorList.add(termNameOrder.reversed());
                else
                    comparatorList.add(termNameOrder);
                break;
            default:
                break;
        }
        return getJoinedComparator(comparatorList);
    }

    public Comparator<ExpressionDetail> getDefaultComparator() {
        List<Comparator<ExpressionDetail>> comparatorList = new ArrayList<>();
        comparatorList.add(termNameOrder);
        comparatorList.add(stageOrder);
        comparatorList.add(assayOrder);
        return getJoinedComparator(comparatorList);
    }

    private static Comparator<ExpressionDetail> termNameOrder =
            Comparator.comparing(annotation -> annotation.getTermName().toLowerCase());

    private static Comparator<ExpressionDetail> stageOrder =
            Comparator.comparing(annotation -> annotation.getStage().getName().toLowerCase());

    private static Comparator<ExpressionDetail> assayOrder =
            Comparator.comparing(annotation -> annotation.getAssay().getName().toLowerCase());

    private static Map<SortingField, Comparator<ExpressionDetail>> sortingFieldMap = new LinkedHashMap<>();

    static {
        sortingFieldMap.put(SortingField.EXPRESSION, termNameOrder);
        sortingFieldMap.put(SortingField.STAGE, stageOrder);
        sortingFieldMap.put(SortingField.ASSAY, assayOrder);
    }

}
