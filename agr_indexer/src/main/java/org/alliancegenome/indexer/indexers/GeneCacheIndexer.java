package org.alliancegenome.indexer.indexers;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.indexer.config.IndexerConfig;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.repository.GeneRepository;

import java.util.ArrayList;

@Log4j2
public class GeneCacheIndexer extends CacheIndexer<Gene> {

    public GeneCacheIndexer(IndexerConfig config) {
        super(config, "gene_cache");
    }

    private GeneRepository geneRepository = new GeneRepository();

    @Override
    protected void index() {

        log.info("Index Gene Running: ");
        ArrayList<Gene> list = (ArrayList<Gene>) geneRepository.getAllGenes();

        list.forEach(gene -> {
            client.add(getKey("walter"), 0, list);
        });

//        addToCache("MGI:97490", list);

//        log.info("Object: " + list2);

    }

    public static void main(String[] args) {
        GeneCacheIndexer ind = new GeneCacheIndexer(IndexerConfig.GeneCacheIndexer);
        Object ob  = ind.client.get(ind.getKey("walter"));
        Gene o = ind.getFromCache("ZFIN:ZDB-GENE-010108-1");
        Gene o1 = ind.getFromCache("MGI:109583");
        log.info("Symbol: " + o.getSymbol());
        String h = "";
    }

}
