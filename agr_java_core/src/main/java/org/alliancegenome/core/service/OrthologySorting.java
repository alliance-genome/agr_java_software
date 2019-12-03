package org.alliancegenome.core.service;

import org.alliancegenome.neo4j.entity.Sorting;
import org.alliancegenome.neo4j.view.OrthologView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class OrthologySorting implements Sorting<OrthologView> {

    private List<Comparator<OrthologView>> defaultList;

    private static Comparator<OrthologView> geneSymbolOrder =
            Comparator.comparing(orthologView -> orthologView.getGene().getSymbol().toLowerCase());

    private static Comparator<OrthologView> homologGeneSymbolOrder =
            Comparator.comparing(orthologView -> orthologView.getHomologGene().getSymbol().toLowerCase());

    private static Comparator<OrthologView> speciesOrder =
            Comparator.comparing(orthologView -> orthologView.getHomologGene().getTaxonId());


    public OrthologySorting() {
        super();

        defaultList = new ArrayList<>(4);
        defaultList.add(geneSymbolOrder);
        defaultList.add(homologGeneSymbolOrder);

    }

    public Comparator<OrthologView> getComparator(SortingField field, Boolean ascending) {
        if (field == null)
            return getJoinedComparator(defaultList);

        switch (field) {
            default:
                return getJoinedComparator(defaultList);
        }
    }


}
