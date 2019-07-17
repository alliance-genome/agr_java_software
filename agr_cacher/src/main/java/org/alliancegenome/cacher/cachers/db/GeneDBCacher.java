package org.alliancegenome.cacher.cachers.db;

import org.alliancegenome.cacher.cachers.Cacher;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;

import java.util.List;

public class GeneDBCacher extends Cacher {

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
