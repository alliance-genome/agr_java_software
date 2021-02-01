package org.alliancegenome.cache.repository.helper;

import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.neo4j.entity.Sorting;
import org.alliancegenome.neo4j.entity.node.Variant;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Comparator.naturalOrder;
import static org.alliancegenome.neo4j.entity.node.Allele.*;

public class AlleleVariantSequenceSorting implements Sorting<AlleleVariantSequence> {

    private List<Comparator<AlleleVariantSequence>> defaultList;
    private List<Comparator<AlleleVariantSequence>> alleleSymbolList;
    private List<Comparator<AlleleVariantSequence>> sequenceFeatureList;
    private List<Comparator<AlleleVariantSequence>> variantHgvsList;
    private List<Comparator<AlleleVariantSequence>> variantList;
    private List<Comparator<AlleleVariantSequence>> variantTypeList;
    private List<Comparator<AlleleVariantSequence>> variantConsequenceList;

    private static Map<String, Integer> categoryMap = new LinkedHashMap<>();

    static {
        categoryMap.put(ALLELE_WITH_ONE_VARIANT, 1);
        categoryMap.put(ALLELE_WITH_MULTIPLE_VARIANT, 2);
        categoryMap.put(CrossReferenceType.ALLELE.getDisplayName(), 3);
        categoryMap.put(CrossReferenceType.VARIANT.getDisplayName(), 4);
    }


    public AlleleVariantSequenceSorting() {
        super();

        defaultList = new ArrayList<>(3);
        defaultList.add(alleleCategoryOrder);
        defaultList.add(alleleSymbolOrder);
        defaultList.add(sequenceFeatureOrder);

        alleleSymbolList = new ArrayList<>(2);
        alleleSymbolList.add(alleleSymbolOrder);

        variantHgvsList = new ArrayList<>(3);
        variantHgvsList.add(variantHgvsNameOrder);
        variantHgvsList.add(alleleSymbolOrder);
        variantHgvsList.add(sequenceFeatureOrder);

        variantConsequenceList = new ArrayList<>(3);
        variantConsequenceList.add(variantConsequenceOrder);
        variantConsequenceList.add(variantTypeOrder);
        variantConsequenceList.add(alleleSymbolOrder);
        variantConsequenceList.add(sequenceFeatureOrder);

        variantTypeList = new ArrayList<>(3);
        variantTypeList.add(variantTypeOrder);
        variantTypeList.add(alleleSymbolOrder);
        variantTypeList.add(sequenceFeatureOrder);

        sequenceFeatureList = new ArrayList<>(3);
        sequenceFeatureList.add(transcriptOrder);
        sequenceFeatureList.add(alleleSymbolOrder);

    }

    public Comparator<AlleleVariantSequence> getComparator(SortingField field, Boolean ascending) {
        if (field == null)
            return getJoinedComparator(defaultList);

        switch (field) {
            case DEFAULT:
                return getJoinedComparator(defaultList);
            case VARIANT_HGVS_NAME:
                return getJoinedComparator(variantHgvsList);
            case VARIANT:
                return getJoinedComparator(variantList);
            case VARIANT_TYPE:
                return getJoinedComparator(variantTypeList);
            case VARIANT_CONSEQUENCE:
                return getJoinedComparator(variantConsequenceList);
            case TRANSCRIPT:
                return getJoinedComparator(sequenceFeatureList);
            default:
                return getJoinedComparator(defaultList);
        }
    }

    static public Comparator<AlleleVariantSequence> alleleCategoryOrder =
            Comparator.comparing(allele -> categoryMap.get(allele.getAllele().getCategory()));

    static public Comparator<AlleleVariantSequence> alleleSymbolOrder =
            Comparator.comparing(allele -> {
                if (allele.getAllele().getSymbolText() == null)
                    return null;
                return allele.getAllele().getSymbolText().toLowerCase();
            }, Comparator.nullsLast(naturalOrder()));

    static public Comparator<AlleleVariantSequence> variantHgvsNameOrder =
            Comparator.comparing(allele -> {
                if (allele.getAllele().getSymbolText() == null)
                    return null;
                return allele.getAllele().getSymbolText().toLowerCase();
            }, Comparator.nullsLast(naturalOrder()));

    static public Comparator<AlleleVariantSequence> sequenceFeatureOrder =
            Comparator.comparing(allele -> {
                if (allele.getConsequence().getTranscriptName() != null)
                    return allele.getConsequence().getTranscriptName();
                return null;
            }, Comparator.nullsLast(naturalOrder()));

    static public Comparator<AlleleVariantSequence> transcriptOrder =
            Comparator.comparing(allele -> {
                if (allele.getConsequence() != null)
                    return allele.getConsequence().getTranscriptName();
                return null;
            }, Comparator.nullsLast(naturalOrder()));

    static public Comparator<AlleleVariantSequence> speciesOrder =
            Comparator.comparing(allele -> {
                if (allele.getAllele().getSymbolText() == null)
                    return null;
                return allele.getAllele().getSymbolText().toLowerCase();
            }, Comparator.nullsLast(naturalOrder()));

    static public Comparator<AlleleVariantSequence> variantOrder =
            Comparator.comparing(allele -> {
                if (CollectionUtils.isEmpty(allele.getAllele().getVariants()))
                    return null;
                String diseaseJoin = allele.getAllele().getVariants().stream().sorted(Comparator.comparing(Variant::getName)).map(Variant::getName).collect(Collectors.joining(""));
                return diseaseJoin.toLowerCase();
            }, Comparator.nullsLast(naturalOrder()));

    static public Comparator<AlleleVariantSequence> variantTypeOrder =
            Comparator.comparing(allele -> {
                if (allele.getVariant() == null)
                    return null;
                return allele.getVariant().getVariantType().getPrimaryKey();
            }, Comparator.nullsLast(naturalOrder()));

    static public Comparator<AlleleVariantSequence> variantConsequenceOrder =
            Comparator.comparing(allele -> {
                if (allele.getConsequence() == null)
                    return null;
                return allele.getConsequence().getTranscriptLevelConsequence();
            }, Comparator.nullsLast(naturalOrder()));

}
