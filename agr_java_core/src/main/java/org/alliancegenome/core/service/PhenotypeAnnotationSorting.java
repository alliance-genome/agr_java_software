package org.alliancegenome.core.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.Sorting;

public class PhenotypeAnnotationSorting implements Sorting<PhenotypeAnnotation> {


    public Comparator<PhenotypeAnnotation> getComparator(SortingField field, Boolean ascending) {
        if (field == null)
            return getDefaultComparator();

        List<Comparator<PhenotypeAnnotation>> comparatorList = new ArrayList<>();
        Comparator<PhenotypeAnnotation> comparator = sortingFieldMap.get(field);
        if (!ascending)
            comparator = comparator.reversed();
        comparatorList.add(comparator);
        sortingFieldMap.keySet().stream()
                // default ordering of phylogenetic and experiment / orthology should not be used.
                // only used for the first time sorting. Any subsequent sorting will ignore that
                .skip(2)
                .filter(sortingField -> !sortingField.equals(field))
                .forEach(sortingField -> comparatorList.add(sortingFieldMap.get(sortingField)));

        return getJoinedComparator(comparatorList);
    }

    public Comparator<PhenotypeAnnotation> getDefaultComparator() {
        List<Comparator<PhenotypeAnnotation>> comparatorList = new ArrayList<>();
        comparatorList.add(phenotypeOrder);

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

    private static Map<SortingField, Comparator<PhenotypeAnnotation>> sortingFieldMap = new LinkedHashMap<>();

    static {
        sortingFieldMap.put(SortingField.PHENOTYPE, phenotypeOrder);
    }

}
