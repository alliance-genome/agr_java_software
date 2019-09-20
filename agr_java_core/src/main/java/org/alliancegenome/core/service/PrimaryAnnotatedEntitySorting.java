package org.alliancegenome.core.service;

import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.Sorting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static java.util.Comparator.naturalOrder;

public class PrimaryAnnotatedEntitySorting implements Sorting<PrimaryAnnotatedEntity> {

    private List<Comparator<PrimaryAnnotatedEntity>> defaultList;
    private List<Comparator<PrimaryAnnotatedEntity>> diseaseList;
    private List<Comparator<PrimaryAnnotatedEntity>> geneList;
    private List<Comparator<PrimaryAnnotatedEntity>> speciesList;

/*
    private static Comparator<PrimaryAnnotatedEntity> diseaseOrder =
            Comparator.comparing(annotation -> annotation.getDisease().getName().toLowerCase());
*/

    private static Comparator<PrimaryAnnotatedEntity> speciesSymbolOrder =
            Comparator.comparing(annotation -> annotation.getSpecies().getName());

    private static Comparator<PrimaryAnnotatedEntity> modelNameSymbolOrder =
            Comparator.comparing(annotation -> annotation.getName());

    public PrimaryAnnotatedEntitySorting() {
        super();

        defaultList = new ArrayList<>(4);
        defaultList.add(modelNameSymbolOrder);

/*
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
*/
    }

    public Comparator<PrimaryAnnotatedEntity> getComparator(SortingField field, Boolean ascending) {
        if (field == null)
            return getJoinedComparator(defaultList);

        switch (field) {
            case GENE:
                return getJoinedComparator(geneList);
            case SPECIES:
                return getJoinedComparator(speciesList);
            case DISEASE:
                return getJoinedComparator(diseaseList);
            default:
                return getJoinedComparator(defaultList);
        }
    }


}
