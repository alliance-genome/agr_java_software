package org.alliancegenome.cacher.cachers.db;

import java.util.List;

import org.alliancegenome.cacher.cachers.DBCacher;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;

public class GeneDBCacher extends DBCacher<Gene> {

    private static GeneRepository geneRepository = new GeneRepository();
    private static List<Gene> allGenes = null;

    public GeneDBCacher(String cacheName) {
        super(cacheName);
    }

    @Override
    protected void cache() {

        allGenes = geneRepository.getAllGenes();
        
        if (allGenes == null)
            return;
        
        for(Gene g: allGenes) {
            cache.put(g.getPrimaryKey(), g);
        }
        
        geneRepository.clearCache();
        
    }

    
}
