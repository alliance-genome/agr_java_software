package org.alliancegenome.cache.repository.helper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.Sorting;

public class  ModelAnnotationsSorting implements Sorting<DiseaseAnnotation> {

	private List<Comparator<DiseaseAnnotation>> defaultList;
	private List<Comparator<DiseaseAnnotation>> diseaseList;
	private List<Comparator<DiseaseAnnotation>> modelList;
	private List<Comparator<DiseaseAnnotation>> speciesList;

/*
	private static Comparator<DiseaseAnnotation> diseaseOrder =
			Comparator.comparing(annotation -> annotation.getDisease().getName().toLowerCase());
*/

	private static Comparator<DiseaseAnnotation> speciesSymbolOrder =
			Comparator.comparing(annotation -> annotation.getModel().getSpecies().getName());

	private static Comparator<DiseaseAnnotation> primaryKeyOrder =
			Comparator.comparing(annotation -> annotation.getModel().getPrimaryKey());

	private static Comparator<DiseaseAnnotation> modelNameSymbolOrder =
			Comparator.comparing(annotation -> annotation.getModel().getName().toLowerCase());

	private static Comparator<DiseaseAnnotation> diseaseOrder =
			Comparator.comparing(annotation -> annotation.getDisease().getName().toLowerCase());


	private static Comparator<DiseaseAnnotation> phylogeneticOrder =
			Comparator.comparing(annotation -> annotation.getModel().getSpecies().getPhylogeneticOrder());

	public ModelAnnotationsSorting() {
		super();

		defaultList = new ArrayList<>(4);
		defaultList.add(phylogeneticOrder);
		defaultList.add(modelNameSymbolOrder);
		defaultList.add(primaryKeyOrder);

		modelList = new ArrayList<>(4);
		modelList.add(modelNameSymbolOrder);
		modelList.add(diseaseOrder);

		diseaseList = new ArrayList<>(4);
		diseaseList.add(diseaseOrder);
		diseaseList.add(phylogeneticOrder);
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

	public Comparator<DiseaseAnnotation> getComparator(SortingField field, Boolean ascending) {
		if (field == null)
			return getJoinedComparator(defaultList);

		switch (field) {
			case MODEL:
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
