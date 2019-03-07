package org.alliancegenome.api.service;

import org.alliancegenome.core.service.SortingField;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.Sorting;

import java.util.*;

public class DiseaseAnnotationSorting implements Sorting<DiseaseAnnotation> {


    Comparator<DiseaseAnnotation> getComparator(SortingField field) {
        if (field == null)
            return getDefaultComparator();

        List<Comparator<DiseaseAnnotation>> comparatorList = new ArrayList<>();
        comparatorList.add(sortingFieldMap.get(field));
        sortingFieldMap.keySet().stream()
                // default ordering of phylogenetic and experiment / orthology should not be used.
                // only used for the first time sorting. Any subsequent sorting will ignore that
                .skip(2)
                .filter(sortingField -> !sortingField.equals(field))
                .forEach(sortingField -> comparatorList.add(sortingFieldMap.get(sortingField)));

        return getJoinedComparator(comparatorList);
    }

    Comparator<DiseaseAnnotation> getDefaultComparator() {
        List<Comparator<DiseaseAnnotation>> comparatorList = new ArrayList<>();
        comparatorList.add(experimentOrthologyOrder);
        comparatorList.add(phylogeneticOrder);
        comparatorList.add(geneSymbolOrder);
        comparatorList.add(diseaseSymbolOrder);

        return getJoinedComparator(comparatorList);
    }

    private static Comparator<DiseaseAnnotation> phylogeneticOrder =
            Comparator.comparing(annotation -> annotation.getGene().getSpecies().getPhylogeneticOrder());

    private static Comparator<DiseaseAnnotation> experimentOrthologyOrder =
            Comparator.comparing(DiseaseAnnotation::getSortOrder);

    private static Comparator<DiseaseAnnotation> geneSymbolOrder =
            Comparator.comparing(annotation -> annotation.getGene().getSymbol().toLowerCase());

    static public Comparator<DiseaseAnnotation> alleleSymbolOrder =
            Comparator.comparing(annotation -> {
                if (annotation.getFeature() == null)
                    return null;
                return annotation.getFeature().getSymbol().toLowerCase();
            }, Comparator.nullsLast(Comparator.naturalOrder()));

    private static Comparator<DiseaseAnnotation> diseaseSymbolOrder =
            Comparator.comparing(annotation -> annotation.getDisease().getName().toLowerCase());

    private static Comparator<DiseaseAnnotation> speciesSymbolOrder =
            Comparator.comparing(annotation -> annotation.getGene().getSpecies().getName());

    private static Map<SortingField, Comparator<DiseaseAnnotation>> sortingFieldMap = new LinkedHashMap<>();

    static {
        sortingFieldMap.put(SortingField.SPECIES_PHYLOGENETIC, phylogeneticOrder);
        sortingFieldMap.put(SortingField.EXPERIMENT_ORTHOLOGY, experimentOrthologyOrder);
        sortingFieldMap.put(SortingField.GENE_SYMBOL, geneSymbolOrder);
        sortingFieldMap.put(SortingField.ALLELE_SYMBOL, alleleSymbolOrder);
        sortingFieldMap.put(SortingField.SPECIES, speciesSymbolOrder);
        sortingFieldMap.put(SortingField.DISEASE, diseaseSymbolOrder);
    }

}
