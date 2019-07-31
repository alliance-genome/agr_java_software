package org.alliancegenome.cache;

import java.io.IOException;
import java.util.List;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.service.JsonResultResponse;
import org.infinispan.client.hotrod.AdminFlag;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.eviction.EvictionType;
import org.infinispan.functional.Param.PersistenceMode;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.core.service.JsonResultResponse;
import lombok.extern.log4j.Log4j2;
import org.alliancegenome.core.service.JsonResultResponse;
import org.ehcache.Cache;
import org.ehcache.PersistentCacheManager;
import org.ehcache.config.builders.CacheConfigurationBuilder;
import org.ehcache.config.builders.CacheManagerBuilder;
import org.ehcache.config.builders.ResourcePoolsBuilder;
import org.ehcache.config.units.MemoryUnit;

import java.io.File;
import java.io.IOException;
import java.util.List;

@Log4j2
public class AllianceCacheManager<T, U extends JsonResultResponse> {

    public static RemoteCacheManager rmc = null;
    public static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        setupCaches();
    }


    public synchronized static void setupCaches() {

        ConfigurationBuilder cb = new ConfigurationBuilder();

        cb.addServers(ConfigHelper.getCacheHost() + ":" + ConfigHelper.getCachePort());

        //      cb.addServer()
        //      .host(ConfigHelper.getCacheHost())
        //      .port(ConfigHelper.getCachePort())
        //      .socketTimeout(5000)
        //      .connectionTimeout(5000)
        //      .tcpNoDelay(true);

        rmc = new RemoteCacheManager(cb.build());

        rmc.start();

        //cache = rmc.administration().withFlags(AdminFlag.PERSISTENT).getOrCreateCache(cacherConfig.getCacheName(), cacherConfig.getCacheTemplate());

        

        //        cb2.persistence()
        //        .passivation(false)
        //        .addSingleFileStore()
        //            .shared(false)
        //            .preload(true)
        //            .fetchPersistentState(true)
        //            .purgeOnStartup(false)
        //            .location("/tmp/" + cacheName)
        //            .async()
        //               .enabled(true)
        //               .threadPoolSize(5);

        //cb2.jmxStatistics();
        //cb2.clustering().cacheMode(CacheMode.LOCAL).create();

        for (CacheAlliance cache : CacheAlliance.values()) {
            log.info("Creating Cache: " + cache.getCacheName());
            
            org.infinispan.configuration.cache.ConfigurationBuilder cb2 = new org.infinispan.configuration.cache.ConfigurationBuilder();
            cb2
            .memory()
            .storageType(StorageType.BINARY)
            .evictionType(EvictionType.MEMORY)
            .size(500_000_000)
            .persistence()
            .passivation(false)
            .addSingleFileStore()
                .preload(true)
                .shared(false)
                .fetchPersistentState(true)
                .location("/data/" + cache.getCacheName()).async().enable().threadPoolSize(5);
            
            rmc.administration().getOrCreateCache(cache.getCacheName(), cb2.build());
            //rmc.getCache(cache.getCacheName()).clear();
            log.info("Cache: " + cache.getCacheName() + " finished creating");
        }

        //return rmc.administration().getOrCreateCache(cacheName, cb2.build());
    }

    //  private synchronized static void setupCache() {
    //      if (rmc != null)
    //          return;
    //
    //      log.info("Setting up persistent cache Manager: ");
    //      File rootDirectory = new File(".", "ehcache-data");
    //      if (web)
    //          rootDirectory = new File("../.", "ehcache-data");
    //      System.out.println("ehcache directory: " + rootDirectory.getAbsolutePath());
    //      CacheManagerBuilder<PersistentCacheManager> cacheManagerBuilder = CacheManagerBuilder.newCacheManagerBuilder()
    //              .with(CacheManagerBuilder.persistence(rootDirectory));
    //      // create individual cache name spaces
    //      for (CacheAlliance cache : CacheAlliance.values()) {
    //          cacheManagerBuilder = cacheManagerBuilder.withCache(cache.getCacheName(), CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, String.class,
    //                  ResourcePoolsBuilder.newResourcePoolsBuilder().disk(8, MemoryUnit.GB, true))
    //          );
    //      }
    //      rmc = cacheManagerBuilder.build(true);
    //  }

    private static RemoteCache<String, String> getCacheSpace(CacheAlliance cache) {
        if (rmc == null)
            setupCaches();
        //log.info("Getting Cache Space: " + cache.getCacheName());
        return rmc.getCache(cache.getCacheName());
    }

    public void putCache(String primaryKey, JsonResultResponse result, Class classView, CacheAlliance cacheSpace) throws JsonProcessingException {
        if (rmc == null)
            setupCaches();
        RemoteCache<String, String> cache = getCacheSpace(cacheSpace);
        String value = mapper.writerWithView(classView).writeValueAsString(result);

        cache.put(primaryKey, value);
    }

    public List<T> getResultListWeb(String entityID, Class classView, Class<? extends JsonResultResponse> clazz, CacheAlliance cacheSpace) {
        return getResultListGeneric(entityID, classView, clazz, cacheSpace, true);
    }

    public List<T> getResultList(String entityID, Class classView, Class<? extends JsonResultResponse> clazz, CacheAlliance cacheSpace) {
        return getResultListGeneric(entityID, classView, clazz, cacheSpace, false);
    }

    private List<T> getResultListGeneric(String entityID, Class classView, Class<? extends JsonResultResponse> clazz, CacheAlliance cacheSpace, boolean web) {

        if (rmc == null) {
            setupCaches();
        }
        String json = getCacheSpace(cacheSpace).get(entityID);
        if (json == null)
            return null;
        JsonResultResponse<T> result;
        try {
            result = mapper.readerWithView(classView).forType(clazz).readValue(json);
        } catch (IOException e) {
            log.error("Error during deserialization ", e);
            throw new RuntimeException(e);
        }
        return result.getResults();
    }

    public static void close() {
        try {
            rmc.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        log.info("closing cache");
    }

}
