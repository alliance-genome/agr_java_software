package org.alliancegenome.cache.repository.helper;

import static java.util.Comparator.naturalOrder;
import static org.alliancegenome.neo4j.entity.node.Allele.*;

import java.util.*;
import java.util.stream.Collectors;

import org.alliancegenome.neo4j.entity.Sorting;
import org.alliancegenome.neo4j.entity.node.*;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

public class AlleleSorting implements Sorting<Allele> {

    private List<Comparator<Allele>> defaultList;
    private List<Comparator<Allele>> alleleSymbolList;
    private List<Comparator<Allele>> speciesList;
    private List<Comparator<Allele>> variantList;
    private List<Comparator<Allele>> variantTypeList;
    private List<Comparator<Allele>> variantConsequenceList;

    private static Map<String, Integer> categoryMap = new LinkedHashMap<>();

    static {
        categoryMap.put(ALLELE_WITH_ONE_VARIANT, 1);
        categoryMap.put(ALLELE_WITH_MULTIPLE_VARIANT, 2);
        categoryMap.put(GeneticEntity.CrossReferenceType.ALLELE.getDisplayName(), 3);
        categoryMap.put(GeneticEntity.CrossReferenceType.VARIANT.getDisplayName(), 4);
    }


    public AlleleSorting() {
        super();

        defaultList = new ArrayList<>(3);
        defaultList.add(alleleCategoryOrder);
        defaultList.add(alleleSymbolOrder);

        alleleSymbolList = new ArrayList<>(2);
        alleleSymbolList.add(alleleSymbolOrder);

        speciesList = new ArrayList<>(3);
        speciesList.add(diseaseOrder);
        speciesList.add(alleleSymbolOrder);

        variantList = new ArrayList<>(3);
        variantList.add(variantOrder);
        variantList.add(alleleSymbolOrder);

        variantTypeList = new ArrayList<>(3);
        variantTypeList.add(variantTypeOrder);
        variantTypeList.add(alleleSymbolOrder);

        variantConsequenceList = new ArrayList<>(3);
        variantConsequenceList.add(variantConsequenceOrder);
        variantConsequenceList.add(variantTypeOrder);
        variantConsequenceList.add(alleleSymbolOrder);

    }

    public Comparator<Allele> getComparator(SortingField field, Boolean ascending) {
        if (field == null)
            return getJoinedComparator(defaultList);

        switch (field) {
            case DEFAULT:
                return getJoinedComparator(defaultList);
            case ALLELESYMBOL:
                return getJoinedComparator(alleleSymbolList);
            case VARIANT:
                return getJoinedComparator(variantList);
            case VARIANT_TYPE:
                return getJoinedComparator(variantTypeList);
            case VARIANT_CONSEQUENCE:
                return getJoinedComparator(variantConsequenceList);
            case SPECIES:
                return getJoinedComparator(speciesList);
            default:
                return getJoinedComparator(defaultList);
        }
    }

    static public Comparator<Allele> alleleCategoryOrder =
            Comparator.comparing(allele -> categoryMap.get(allele.getCategory()));

    static public Comparator<Allele> alleleSymbolOrder =
            Comparator.comparing(allele -> {
                if (allele.getSymbolText() == null)
                    return null;
                return allele.getSymbolText().toLowerCase();
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

    static public Comparator<Allele> variantOrder =
            Comparator.comparing(allele -> {
                if (CollectionUtils.isEmpty(allele.getVariants()))
                    return null;
                String diseaseJoin = allele.getVariants().stream().sorted(Comparator.comparing(Variant::getName)).map(Variant::getName).collect(Collectors.joining(""));
                return diseaseJoin.toLowerCase();
            }, Comparator.nullsLast(naturalOrder()));

    static public Comparator<Allele> variantTypeOrder =
            Comparator.comparing(allele -> {
                if (CollectionUtils.isEmpty(allele.getVariants()))
                    return null;
                String diseaseJoin = allele.getVariants().stream().sorted(Comparator.comparing(variant -> variant.getVariantType().getName())).map(variant -> variant.getVariantType().getName()).collect(Collectors.joining(""));
                return diseaseJoin.toLowerCase();
            }, Comparator.nullsLast(naturalOrder()));

    static public Comparator<Allele> variantConsequenceOrder =
            Comparator.comparing(allele -> {
                if (CollectionUtils.isEmpty(allele.getVariants()))
                    return null;
                String diseaseJoin = allele.getVariants().stream()
                        .filter(variant -> variant.getGeneLevelConsequence() != null)
                        .sorted(Comparator.comparing(variant -> variant.getGeneLevelConsequence().getGeneLevelConsequence()))
                        .map(variant -> variant.getGeneLevelConsequence().getGeneLevelConsequence()).collect(Collectors.joining(""));
                return StringUtils.isNotEmpty(diseaseJoin) ? diseaseJoin.toLowerCase() : null;
            }, Comparator.nullsLast(naturalOrder()));

    static public Comparator<Allele> phenotypeStatementOrder =
            Comparator.comparing(allele -> {
                if (CollectionUtils.isEmpty(allele.getPhenotypes()))
                    return null;
                String phenoJoin = allele.getPhenotypes().stream().sorted(Comparator.comparing(Phenotype::getPhenotypeStatement)).map(Phenotype::getPhenotypeStatement).collect(Collectors.joining(""));
                return phenoJoin.toLowerCase();

            }, Comparator.nullsLast(naturalOrder()));

}
