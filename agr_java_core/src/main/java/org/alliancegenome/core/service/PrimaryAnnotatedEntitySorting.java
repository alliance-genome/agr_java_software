package org.alliancegenome.core.service;

import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.Sorting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class PrimaryAnnotatedEntitySorting implements Sorting<PrimaryAnnotatedEntity> {

    private List<Comparator<PrimaryAnnotatedEntity>> defaultList;
    private List<Comparator<PrimaryAnnotatedEntity>> diseaseList;
    private List<Comparator<PrimaryAnnotatedEntity>> modelList;
    private List<Comparator<PrimaryAnnotatedEntity>> speciesList;

/*
    private static Comparator<PrimaryAnnotatedEntity> diseaseOrder =
            Comparator.comparing(annotation -> annotation.getDisease().getName().toLowerCase());
*/

    private static Comparator<PrimaryAnnotatedEntity> modelNameSymbolOrder =
            Comparator.comparing(annotation -> annotation.getName().toLowerCase());

/*
    private static Comparator<PrimaryAnnotatedEntity> diseaseOrder =
            Comparator.comparing(annotation -> annotation.getDisease().getName().toLowerCase());
*/



    public PrimaryAnnotatedEntitySorting() {
        super();

        defaultList = new ArrayList<>(4);
        defaultList.add(modelNameSymbolOrder);

        modelList = new ArrayList<>(4);
        modelList.add(modelNameSymbolOrder);
//        modelList.add(diseaseOrder);

        diseaseList = new ArrayList<>(4);
/*
        diseaseList.add(diseaseOrder);
*/
        diseaseList.add(modelNameSymbolOrder);
/*

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
            case MODEL_NAME:
                return getJoinedComparator(modelList);
            case SPECIES:
                return getJoinedComparator(speciesList);
            case DISEASE:
                return getJoinedComparator(diseaseList);
            default:
                return getJoinedComparator(defaultList);
        }
    }


}
