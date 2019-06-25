package org.alliancegenome.cacher.cachers;

import java.util.List;

import org.alliancegenome.cacher.config.CacherConfig;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class GeneCacher extends Cacher<Gene> {

    private static GeneRepository geneRepository = new GeneRepository();
    private static List<Gene> allGenes = null;

    public GeneCacher(CacherConfig cacherConfig) {
        super(cacherConfig);
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
