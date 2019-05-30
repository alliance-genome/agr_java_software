package org.alliancegenome.core.service;

import org.alliancegenome.neo4j.entity.Sorting;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.SimpleTerm;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Comparator.naturalOrder;

public class AlleleSorting implements Sorting<Allele> {

    private List<Comparator<Allele>> defaultList;
    private List<Comparator<Allele>> diseaseList;

    public AlleleSorting() {
        super();

        defaultList = new ArrayList<>(2);
        defaultList.add(alleleSymbolOrder);

        diseaseList = new ArrayList<>(2);
        diseaseList.add(diseaseOrder);
        diseaseList.add(alleleSymbolOrder);

    }

    public Comparator<Allele> getComparator(SortingField field, Boolean ascending) {
        if (field == null)
            return getJoinedComparator(defaultList);

        switch (field) {
            case DEFAULT:
                return getJoinedComparator(defaultList);
            case ALLELESYMBOL:
                return getJoinedComparator(defaultList);
            case DISEASE:
                return getJoinedComparator(diseaseList);
            default:
                return getJoinedComparator(defaultList);
        }
    }

    static public Comparator<Allele> alleleSymbolOrder =
            Comparator.comparing(allele -> {
                if (allele.getSymbolText() == null)
                    return null;
                return allele.getSymbolText().toLowerCase();
            }, Comparator.nullsLast(naturalOrder()));

    static public Comparator<Allele> diseaseOrder =
            Comparator.comparing(allele -> {
                if (CollectionUtils.isEmpty(allele.getDiseases()))
                    return null;
                String diseaseJoin = allele.getDiseases().stream().sorted(Comparator.comparing(SimpleTerm::getName)).map(SimpleTerm::getName).collect(Collectors.joining(""));
                return diseaseJoin.toLowerCase();
            }, Comparator.nullsLast(naturalOrder()));

}
