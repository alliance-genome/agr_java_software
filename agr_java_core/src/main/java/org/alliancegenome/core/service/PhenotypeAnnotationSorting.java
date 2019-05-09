package org.alliancegenome.core.service;

import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.Sorting;

import java.util.*;

public class PhenotypeAnnotationSorting implements Sorting<PhenotypeAnnotation> {


    public Comparator<PhenotypeAnnotation> getComparator(SortingField field, Boolean ascending) {
        if (field == null)
            return getDefaultComparator();

        List<Comparator<PhenotypeAnnotation>> comparatorList = new ArrayList<>();
        switch (field) {
            case PHENOTYPE:
                return getDefaultComparator();
            case GENETIC_ENTITY:
                comparatorList.add(geneticEntityTypeOrder);
                comparatorList.add(geneticEntityOrder);
                comparatorList.add(phenotypeOrder);
                break;
            default:
                break;
        }
        return getJoinedComparator(comparatorList);
    }

    public Comparator<PhenotypeAnnotation> getDefaultComparator() {
        List<Comparator<PhenotypeAnnotation>> comparatorList = new ArrayList<>();
        comparatorList.add(phenotypeOrder);
        comparatorList.add(geneticEntityTypeOrder);
        comparatorList.add(geneticEntityOrder);
        return getJoinedComparator(comparatorList);
    }

/*
    static public Comparator<PhenotypeAnnotation> alleleSymbolOrder =
            Comparator.comparing(annotation -> {
                if (annotation.getFeature() == null)
                    return null;
                return annotation.getFeature().getSymbol().toLowerCase();
            }, Comparator.nullsLast(naturalOrder()));
*/

    private static Comparator<PhenotypeAnnotation> phenotypeOrder =
            Comparator.comparing(annotation -> annotation.getPhenotype().toLowerCase());

    private static Comparator<PhenotypeAnnotation> geneticEntityOrder =
            Comparator.comparing(annotation -> annotation.getGeneticEntity().getSymbol().toLowerCase()
            );

    private static Comparator<PhenotypeAnnotation> geneticEntityTypeOrder =
            Comparator.comparing(annotation -> annotation.getGeneticEntity().getType());

    private static Map<SortingField, Comparator<PhenotypeAnnotation>> sortingFieldMap = new LinkedHashMap<>();

    static {
        sortingFieldMap.put(SortingField.PHENOTYPE, phenotypeOrder);
        sortingFieldMap.put(SortingField.GENETIC_ENTITY_TYPE, geneticEntityTypeOrder);
        sortingFieldMap.put(SortingField.GENETIC_ENTITY, geneticEntityOrder);
    }

}
