package org.alliancegenome.cacher;

import org.alliancegenome.core.ExpressionDetail;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.configuration.cache.CacheMode;

import java.util.List;

public class Main2 {

    public static void main(String[] args) {


        ConfigurationBuilder cb = new ConfigurationBuilder();

        cb.addServer()
        .host("localhost")
        .port(11222)
        .socketTimeout(500)
        .connectionTimeout(500)
        .tcpNoDelay(true);

        RemoteCacheManager rmc = new RemoteCacheManager(cb.build());

        //cache = rmc.administration().withFlags(AdminFlag.PERMANENT).getOrCreateCache(cacherConfig.getCacheName(), cacherConfig.getCacheTemplate());

        org.infinispan.configuration.cache.ConfigurationBuilder cb2 = new org.infinispan.configuration.cache.ConfigurationBuilder();

//      cb2.persistence()
//      .passivation(false)
//      .addSingleFileStore()
//      .shared(false)
//      .preload(true)
//      .fetchPersistentState(true)
//      .purgeOnStartup(false)
//      .location("/tmp/gene")
//      .async()
//      .enabled(true)
//      .threadPoolSize(5);
        //rmc.administration().removeCache("gene");

        cb2.jmxStatistics();
        cb2.clustering().cacheMode(CacheMode.LOCAL).create();
        
        //RemoteCache<String, List<ExpressionDetail>> cache = rmc.administration().getOrCreateCache("geneExpression", cb2.build());
        //RemoteCache<String, List<PhenotypeAnnotation>> cache = rmc.administration().getOrCreateCache("genePhenotypeDBCacher", cb2.build());
        RemoteCache<String,Allele> cacheAllele = rmc.administration().getOrCreateCache("allele", cb2.build());
        RemoteCache<String, List<Allele>> cacheGeneAllele = rmc.administration().getOrCreateCache("geneAllele", cb2.build());
        RemoteCache<String, List<Allele>> cachetaxonAllele = rmc.administration().getOrCreateCache("taxonAllele", cb2.build());
        RemoteCache<String, List<Allele>> cachePhenotype = rmc.administration().getOrCreateCache("genePhenotype", cb2.build());

        System.out.println(cacheAllele.size());
        System.out.println(cacheGeneAllele.size());
        System.out.println(cachetaxonAllele.size());
        System.out.println(cachePhenotype.size());
        System.out.println(cachetaxonAllele.get("NCBITaxon:7955").size());
        //System.out.println(cachetaxonAllele.get("NCBITaxon:7955").get(0).getSymbolText());
        System.out.println(cacheGeneAllele.get("MGI:109583").get(0).getSymbolText());
        //rmc.administration().reindexCache("gene");
        //rmc.start();
        //cache.put("BlahKey", new Gene());
/*
        List<Allele> details = cache.get("MGI:109583");
        System.out.println(details.size());
*/
    }

}
//cachetaxonAllele.get("NCBITaxon:7955")