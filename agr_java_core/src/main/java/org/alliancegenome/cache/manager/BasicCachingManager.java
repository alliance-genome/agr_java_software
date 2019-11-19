package org.alliancegenome.cache.manager;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.CollectionType;
import lombok.extern.log4j.Log4j2;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.core.config.ConfigHelper;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.client.hotrod.configuration.ConfigurationBuilder;
import org.infinispan.configuration.cache.StorageType;
import org.infinispan.eviction.EvictionType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public class BasicCachingManager<O> {

    public static RemoteCacheManager rmc = null;
    public static ObjectMapper mapper = new ObjectMapper();

    private Class<O> type;

    public BasicCachingManager(Class<O> clazz) {
        type = clazz;
    }

    static {
        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        //mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        setupCaches();
    }

    private synchronized static void setupCaches() {


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

        ConfigurationBuilder cb = new ConfigurationBuilder();

        cb.addServer()
                .host(ConfigHelper.getCacheHost())
                .port(ConfigHelper.getCachePort())
                .socketTimeout(500000)
                .connectionTimeout(500000)
                .tcpNoDelay(true);

        rmc = new RemoteCacheManager(cb.build());

        rmc.start();

        for (CacheAlliance cache : CacheAlliance.values()) {
            log.debug("Creating Cache: " + cache.getCacheName());

            org.infinispan.configuration.cache.ConfigurationBuilder cb2 = new org.infinispan.configuration.cache.ConfigurationBuilder();

            cb2
                    .memory()
                    .storageType(StorageType.BINARY)
                    .evictionType(EvictionType.MEMORY)
                    .size(1_500_000_000)
                    .persistence()
                    .passivation(false)
                    .addSingleFileStore()
                    .preload(true)
                    .shared(false)
                    .fetchPersistentState(true)
                    .location("/data/" + cache.getCacheName()).async().enable().threadPoolSize(5);


            rmc.administration().getOrCreateCache(cache.getCacheName(), cb2.build());

            // log.info("Clearing cache if exists"); // This might need to run if the cacher is running
            // rmc.getCache(cache.getCacheName()).clear(); // But should not be run via the API

            log.debug("Cache: " + cache.getCacheName() + " finished creating");
        }

    }

    private RemoteCache<String, String> getCacheSpace(CacheAlliance cache) {
        //log.info("Getting Cache Space: " + cache.getCacheName());
        return rmc.getCache(cache.getCacheName());
    }

    public void putCache(String primaryKey, String o, CacheAlliance cacheSpace) throws JsonProcessingException {
        RemoteCache<String, String> cache = getCacheSpace(cacheSpace);
        cache.put(primaryKey, o);
    }

    public List<O> getCache(String entityID, CacheAlliance cacheSpace) {
        CollectionType javaType = mapper.getTypeFactory()
                .constructCollectionType(List.class, type);

        String json = getCacheSpace(cacheSpace).get(entityID);
        if (json == null)
            return null;

        List<O> list;

        try {
            list = new ArrayList<>(BasicCachingManager.mapper.readValue(json, javaType));
        } catch (IOException e) {
            log.error("Error during deserialization ", e);
            throw new RuntimeException(e);
        }

        return list;
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

    public void setCache(String primaryKey, List items, Class<?> classView, CacheAlliance cacheAlliance) throws JsonProcessingException {
        RemoteCache<String, String> cache = rmc.getCache(cacheAlliance.getCacheName());
        String value = mapper.writerWithView(classView).writeValueAsString(items);
        cache.put(primaryKey, value);
    }
}
