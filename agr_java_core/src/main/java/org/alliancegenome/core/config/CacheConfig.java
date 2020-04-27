package org.alliancegenome.core.config;

import org.alliancegenome.cache.CacheAlliance;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.eviction.EvictionType;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class CacheConfig {

    private static RemoteCacheManager manager;
    
    public static RemoteCacheManager defaultRemoteCacheManager() {
        if(manager != null) return manager;
        
        log.info("Setting up cache manager");
        ConfigurationBuilder builder = new ConfigurationBuilder()
                .addServer()
                .host(ConfigHelper.getCacheHost())
                .port(ConfigHelper.getCachePort())
                //.security().authentication().saslMechanism("PLAIN").username("user").password("pass")
                .socketTimeout(500000)
                .connectionTimeout(500000)
                .tcpNoDelay(true);
        
        log.info("ConfigurationBuilder: " + builder);
        manager = new RemoteCacheManager(builder.build());
        return manager;
    }
    
    public static RemoteCache<String, String> createCache(CacheAlliance cache) {

        if(manager == null) defaultRemoteCacheManager();
        
        log.debug("Creating Cache: " + cache.getCacheName());

        org.infinispan.configuration.cache.ConfigurationBuilder cb2 = new org.infinispan.configuration.cache.ConfigurationBuilder();

        cb2
                .memory()
                .storageType(StorageType.BINARY)
                .evictionType(EvictionType.MEMORY)
                .size(100_000).expiration().lifespan(-1)
                .persistence()
                .passivation(false)
                .addSingleFileStore().purgeOnStartup(false)
                .preload(false)
                .shared(false)
                .fetchPersistentState(true)
                .location("/tmp/data/" + cache.getCacheName()).async().enable().threadPoolSize(5);

        RemoteCache<String, String> remoteCache = manager.administration().getOrCreateCache(cache.getCacheName(), cb2.build());

        // log.info("Clearing cache if exists"); // This might need to run if the cacher is running
        // rmc.getCache(cache.getCacheName()).clear(); // But should not be run via the API

        log.debug("Cache: " + cache.getCacheName() + " finished creating");
        return remoteCache;
        
    }

    //  public CacheProvider() {
    //      log.info("Setting up cache manager");
    //      ServerConfigurationBuilder builder = new ConfigurationBuilder().addServer().host("localhost").port(11222);
    //      log.info("ServerConfigurationBuilder: " + builder);
    //      manager = new RemoteCacheManager(builder.build());
    //  }
    //
    //  @Override
    //  public RemoteCacheManager getContext(Class<?> type) {
    //      return manager;
    //  }


}
