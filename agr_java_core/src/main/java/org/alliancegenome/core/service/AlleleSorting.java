package org.alliancegenome.core.service;

import org.alliancegenome.neo4j.entity.Sorting;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.SimpleTerm;
import org.alliancegenome.neo4j.entity.node.Variant;
import org.alliancegenome.neo4j.entity.node.Phenotype;

import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static java.util.Comparator.naturalOrder;

public class AlleleSorting implements Sorting<Allele> {

    private List<Comparator<Allele>> defaultList;
    private List<Comparator<Allele>> diseaseList;
    private List<Comparator<Allele>> speciesList;

    public AlleleSorting() {
        super();

        defaultList = new ArrayList<>(3);
        defaultList.add(variantExistOrder);
        defaultList.add(alleleSymbolOrder);
        defaultList.add(phenotypeStatementOrder);

        diseaseList = new ArrayList<>(3);
        diseaseList.add(diseaseOrder);
        diseaseList.add(alleleSymbolOrder);

        speciesList = new ArrayList<>(3);
        speciesList.add(diseaseOrder);
        speciesList.add(alleleSymbolOrder);

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
            case SPECIES:
                return getJoinedComparator(speciesList);
            default:
                return getJoinedComparator(defaultList);
        }
    }

    static public Comparator<Allele> variantExistOrder =
            Comparator.comparing(allele -> {
                if (CollectionUtils.isEmpty(allele.getVariants()))
                    return null;
                String variantJoin = allele.getVariants().stream().sorted(Comparator.comparing(Variant::getName)).map(Variant::getName).collect(Collectors.joining(""));
                return variantJoin.toLowerCase();
            }, Comparator.nullsLast(naturalOrder()));

    static public Comparator<Allele> alleleSymbolOrder =
            Comparator.comparing(allele -> {
                if (allele.getSymbolText() == null)
                    return null;
                return allele.getSymbolText().toLowerCase();
            }, Comparator.nullsLast(naturalOrder()));

    static public Comparator<Allele> phenotypeStatementOrder =
            Comparator.comparing(allele -> {
                if (CollectionUtils.isEmpty(allele.getPhenotypes()))
                    return null;
                String phenoJoin = allele.getPhenotypes().stream().sorted(Comparator.comparing(Phenotype::getPhenotypeStatement)).map(Phenotype::getPhenotypeStatement).collect(Collectors.joining(""));
                return phenoJoin.toLowerCase();

            }, Comparator.nullsLast(naturalOrder()));

    static public Comparator<Allele> speciesOrder =
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
