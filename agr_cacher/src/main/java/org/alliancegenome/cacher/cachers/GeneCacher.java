package org.alliancegenome.cacher.cachers;

import java.util.List;

import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;

public class GeneCacher extends Cacher {

    private static GeneRepository geneRepository = new GeneRepository();
    private static List<Gene> allGenes = null;

    @Override
    protected void cache() {

        allGenes = geneRepository.getAllGenes();

        if (allGenes == null)
            return;
        
/*
        for(Gene g: allGenes) {
            cache.put(g.getPrimaryKey(), g);
        }
*/

        geneRepository.clearCache();

    }


}
