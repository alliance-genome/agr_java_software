package org.alliancegenome.cache.repository.helper;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.alliancegenome.neo4j.entity.Sorting;
import org.alliancegenome.neo4j.view.HomologView;

public class OrthologySorting implements Sorting<HomologView> {

	private List<Comparator<HomologView>> defaultList;

	private static Comparator<HomologView> geneSymbolOrder =
			Comparator.comparing(orthologView -> orthologView.getGene().getSymbol().toLowerCase());

	private static Comparator<HomologView> homologGeneSymbolOrder =
			Comparator.comparing(orthologView -> orthologView.getHomologGene().getSymbol().toLowerCase());

	private static Comparator<HomologView> speciesOrder =
			Comparator.comparing(orthologView -> {
				if (orthologView.getHomologGene().getSpecies() != null)
					return orthologView.getHomologGene().getSpecies().getPhylogeneticOrder();
				return -1;
			});


	public OrthologySorting() {
		super();

		defaultList = new ArrayList<>(4);
		defaultList.add(speciesOrder);
		defaultList.add(geneSymbolOrder);
		defaultList.add(homologGeneSymbolOrder);

	}

	public Comparator<HomologView> getComparator(SortingField field, Boolean ascending) {
		if (field == null)
			return getJoinedComparator(defaultList);

		switch (field) {
			default:
				return getJoinedComparator(defaultList);
		}
	}


}
