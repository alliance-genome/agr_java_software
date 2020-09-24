package org.alliancegenome.cache.repository.helper;

import java.util.*;

import org.alliancegenome.neo4j.entity.*;
import org.apache.commons.collections.CollectionUtils;

public class PrimaryAnnotatedEntitySorting implements Sorting<PrimaryAnnotatedEntity> {

    private List<Comparator<PrimaryAnnotatedEntity>> defaultList;
    private List<Comparator<PrimaryAnnotatedEntity>> diseaseList;
    private List<Comparator<PrimaryAnnotatedEntity>> modelList;

    private static Comparator<PrimaryAnnotatedEntity> modelNameSymbolOrder =
            Comparator.comparing(annotation -> annotation.getName().toLowerCase());

    private static Comparator<PrimaryAnnotatedEntity> primaryKeyOrder =
            Comparator.comparing(PrimaryAnnotatedEntity::getId);

    private static Comparator<PrimaryAnnotatedEntity> diseaseExistOrder =
            Comparator.comparing(annotation -> CollectionUtils.isEmpty(annotation.getDiseaseModels()));

    private static Comparator<PrimaryAnnotatedEntity> phenotypeExistsOrder =
            Comparator.comparing(annotation -> CollectionUtils.isEmpty(annotation.getPhenotypes()));

    public PrimaryAnnotatedEntitySorting() {
        super();

        defaultList = new ArrayList<>(4);
        defaultList.add(diseaseExistOrder);
        defaultList.add(phenotypeExistsOrder);
        defaultList.add(modelNameSymbolOrder);
        defaultList.add(primaryKeyOrder);

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
            case MODEL:
                return getJoinedComparator(modelList);
            case DISEASE:
                return getJoinedComparator(diseaseList);
            default:
                return getJoinedComparator(defaultList);
        }
    }


}
