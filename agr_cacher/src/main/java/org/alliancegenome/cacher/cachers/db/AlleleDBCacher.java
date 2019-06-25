package org.alliancegenome.cacher.cachers.db;

import java.util.Set;

import org.alliancegenome.cacher.cachers.DBCacher;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.repository.AlleleRepository;

public class AlleleDBCacher extends DBCacher<Allele> {

    private static AlleleRepository alleleRepository = new AlleleRepository();
    private static Set<Allele> allAlleles = null;

    public AlleleDBCacher(String cacheName) {
        super(cacheName);
    }

    @Override
    protected void cache() {

        allAlleles = alleleRepository.getAllAlleles();
        
        if (allAlleles == null)
            return;
        
        for(Allele a: allAlleles) {
            cache.put(a.getPrimaryKey(), a);
        }
        
        alleleRepository.clearCache();
    }

}
