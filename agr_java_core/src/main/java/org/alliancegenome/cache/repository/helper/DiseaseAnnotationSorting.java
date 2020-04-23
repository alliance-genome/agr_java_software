package org.alliancegenome.cache.repository.helper;

import static java.util.Comparator.naturalOrder;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.Sorting;

public class DiseaseAnnotationSorting implements Sorting<DiseaseAnnotation> {

    private List<Comparator<DiseaseAnnotation>> defaultList;
    private List<Comparator<DiseaseAnnotation>> diseaseList;
    private List<Comparator<DiseaseAnnotation>> alleleList;
    private List<Comparator<DiseaseAnnotation>> geneList;
    private List<Comparator<DiseaseAnnotation>> speciesList;
    private List<Comparator<DiseaseAnnotation>> alleleDefaultList;

    private static Comparator<DiseaseAnnotation> phylogeneticOrder =
            Comparator.comparing(annotation -> {
                if (annotation.getGene() == null)
                    return 1;
                if (annotation.getGene().getSpecies() == null)
                    return 1;
                return annotation.getGene().getSpecies().getPhylogeneticOrder();
            });

    private static Comparator<DiseaseAnnotation> phylogeneticAlleleOrder =
            Comparator.comparing(annotation -> {
                if (annotation.getFeature() == null)
                    return 1;
                if (annotation.getFeature().getSpecies() == null)
                    return 1;
                return annotation.getFeature().getSpecies().getPhylogeneticOrder();
            });

    private static Comparator<DiseaseAnnotation> experimentOrthologyOrder =
            Comparator.comparing(DiseaseAnnotation::getSortOrder);

    private static Comparator<DiseaseAnnotation> geneSymbolOrder =
            Comparator.comparing(annotation -> {
                if (annotation.getGene() == null)
                    return null;
                return annotation.getGene().getSymbol().toLowerCase();
            }, Comparator.nullsLast(naturalOrder()));

    static public Comparator<DiseaseAnnotation> alleleSymbolOrder =
            Comparator.comparing(annotation -> {
                if (annotation.getFeature() == null)
                    return null;
                return annotation.getFeature().getSymbol().toLowerCase();
            }, Comparator.nullsLast(naturalOrder()));

    private static Comparator<DiseaseAnnotation> diseaseOrder =
            Comparator.comparing(annotation -> annotation.getDisease().getName().toLowerCase());

    private static Comparator<DiseaseAnnotation> speciesSymbolOrder =
            Comparator.comparing(annotation -> {
                if (annotation.getModel() != null)
                    return annotation.getModel().getSpecies().getName().toLowerCase();
                else if (annotation.getGene() != null)
                    return annotation.getGene().getSpecies().getName().toLowerCase();
                else if (annotation.getFeature() != null)
                    return annotation.getFeature().getSpecies().getName().toLowerCase();
                return "";
            }, Comparator.nullsLast(naturalOrder()));

    private static Comparator<DiseaseAnnotation> associationTypeOrder =
            Comparator.comparing(DiseaseAnnotation::getAssociationType);

    public DiseaseAnnotationSorting() {
        super();

        defaultList = new ArrayList<>(4);
        defaultList.add(experimentOrthologyOrder);
        defaultList.add(phylogeneticOrder);
        defaultList.add(geneSymbolOrder);
        defaultList.add(diseaseOrder);
        defaultList.add(alleleSymbolOrder);

        alleleList = new ArrayList<>(4);
        alleleList.add(alleleSymbolOrder);
        alleleList.add(diseaseOrder);
        alleleList.add(experimentOrthologyOrder);
        alleleList.add(phylogeneticOrder);
        alleleList.add(geneSymbolOrder);

        alleleDefaultList = new ArrayList<>(4);
        alleleDefaultList.add(phylogeneticAlleleOrder);
        alleleDefaultList.add(alleleSymbolOrder);
        alleleDefaultList.add(diseaseOrder);

        diseaseList = new ArrayList<>(4);
        diseaseList.add(diseaseOrder);
        diseaseList.add(phylogeneticOrder);
        diseaseList.add(geneSymbolOrder);
        diseaseList.add(alleleSymbolOrder);

        geneList = new ArrayList<>(4);
        geneList.add(geneSymbolOrder);
        geneList.add(diseaseOrder);
        geneList.add(phylogeneticOrder);
        geneList.add(alleleSymbolOrder);

        speciesList = new ArrayList<>(4);
        speciesList.add(speciesSymbolOrder);
        speciesList.add(geneSymbolOrder);
        speciesList.add(diseaseOrder);
        speciesList.add(alleleSymbolOrder);
    }

    public Comparator<DiseaseAnnotation> getComparator(SortingField field, Boolean ascending) {
        if (field == null)
            return getJoinedComparator(defaultList);

        switch (field) {
            case GENE:
                return getJoinedComparator(geneList);
            case SPECIES:
                return getJoinedComparator(speciesList);
            case DISEASE:
                return getJoinedComparator(diseaseList);
            case ALLELE:
                return getJoinedComparator(alleleList);
            case DISEASE_ALLELE_DEFAULT:
                return getJoinedComparator(alleleDefaultList);
            default:
                return getJoinedComparator(defaultList);
        }
    }


/*
    public Comparator<DiseaseAnnotation> getComparator(SortingField field, Boolean ascending) {
        if (field == null)
            return getDefaultComparator();

        List<Comparator<DiseaseAnnotation>> comparatorList = new ArrayList<>();
        Comparator<DiseaseAnnotation> comparator = sortingFieldMap.get(field);
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
*/

}
