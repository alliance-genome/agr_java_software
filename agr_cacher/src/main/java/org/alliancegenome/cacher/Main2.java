package org.alliancegenome.cacher;

import org.alliancegenome.neo4j.entity.node.Gene;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.configuration.cache.CacheMode;

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
        
        RemoteCache<String, Gene> cache = rmc.administration().getOrCreateCache("gene", cb2.build());
        
        System.out.println(cache.size());
        //rmc.administration().reindexCache("gene");
        //rmc.start();
        cache.put("BlahKey", new Gene());
        Gene g = cache.get("BlahKey");
        System.out.println(g);
    }

}
