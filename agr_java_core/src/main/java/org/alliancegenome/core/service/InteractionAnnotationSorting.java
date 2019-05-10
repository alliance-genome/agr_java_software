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
                comparatorList.add(interactorGeneSymbolOrder);
                break;
            default:
                break;
        }
        return getJoinedComparator(comparatorList);
    }

    public Comparator<InteractionGeneJoin> getDefaultComparator() {
        List<Comparator<InteractionGeneJoin>> comparatorList = new ArrayList<>();
        comparatorList.add(interactorGeneSymbolOrder);
        return getJoinedComparator(comparatorList);
    }

/*
    static public Comparator<InteractionGeneJoin> alleleSymbolOrder =
            Comparator.comparing(annotation -> {
                if (annotation.getFeature() == null)
                    return null;
                return annotation.getFeature().getSymbol().toLowerCase();
            }, Comparator.nullsLast(naturalOrder()));
*/

    private static Comparator<InteractionGeneJoin> interactorGeneSymbolOrder =
            Comparator.comparing(annotation -> {
                return annotation.getGeneB().getSymbol().toLowerCase();
            });

    /*
        private static Comparator<InteractionGeneJoin> phenotypeOrder =
                Comparator.comparing(annotation -> annotation.getPhenotype().toLowerCase());

        private static Comparator<InteractionGeneJoin> geneticEntityOrder =
                Comparator.comparing(annotation -> annotation.getGeneticEntity().getSymbol().toLowerCase()
                );

        private static Comparator<InteractionGeneJoin> geneticEntityTypeOrder =
                Comparator.comparing(annotation -> annotation.getGeneticEntity().getType());

    */
    private static Map<SortingField, Comparator<InteractionGeneJoin>> sortingFieldMap = new LinkedHashMap<>();

    static {
        sortingFieldMap.put(SortingField.INTERACTOR_GENE_SYMBOL, interactorGeneSymbolOrder);
    }

}
