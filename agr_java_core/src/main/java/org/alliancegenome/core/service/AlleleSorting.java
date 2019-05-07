package org.alliancegenome.core.service;

import org.alliancegenome.neo4j.entity.Sorting;
import org.alliancegenome.neo4j.entity.node.Allele;

import java.util.*;

import static java.util.Comparator.naturalOrder;

public class AlleleSorting implements Sorting<Allele> {


    public Comparator<Allele> getComparator(SortingField field, Boolean ascending) {
        if (field == null)
            return getDefaultComparator();

        List<Comparator<Allele>> comparatorList = new ArrayList<>();
        Comparator<Allele> comparator = sortingFieldMap.get(field);
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

    public Comparator<Allele> getDefaultComparator() {
        List<Comparator<Allele>> comparatorList = new ArrayList<>();
        comparatorList.add(alleleSymbolOrder);

        return getJoinedComparator(comparatorList);
    }

    static public Comparator<Allele> alleleSymbolOrder =
            Comparator.comparing(allele -> {
                if (allele.getSymbolText() == null)
                    return null;
                return allele.getSymbolText().toLowerCase();
            }, Comparator.nullsLast(naturalOrder()));

    private static Map<SortingField, Comparator<Allele>> sortingFieldMap = new LinkedHashMap<>();

    static {
        sortingFieldMap.put(SortingField.SYMBOL, alleleSymbolOrder);
    }

}
