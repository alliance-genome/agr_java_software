package org.alliancegenome.cache;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    private static PersistentCacheManager persistentCacheManager = null;
    private static boolean web;

    public static ObjectMapper mapper = new ObjectMapper();

    static {
        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
    }

    private synchronized static void setupCache() {
        if (persistentCacheManager != null)
            return;

        log.info("Setting up persistent cache Manager: ");
        File rootDirectory = new File(".", "ehcache-data");
        if (web)
            rootDirectory = new File("../.", "ehcache-data");
        System.out.println("ehcache directory: " + rootDirectory.getAbsolutePath());
        CacheManagerBuilder<PersistentCacheManager> cacheManagerBuilder = CacheManagerBuilder.newCacheManagerBuilder()
                .with(CacheManagerBuilder.persistence(rootDirectory));
        // create individual cache name spaces
        for (CacheAlliance cache : CacheAlliance.values()) {
            cacheManagerBuilder = cacheManagerBuilder.withCache(cache.getCacheName(), CacheConfigurationBuilder.newCacheConfigurationBuilder(String.class, String.class,
                    ResourcePoolsBuilder.newResourcePoolsBuilder().disk(8, MemoryUnit.GB, true))
            );
        }
        persistentCacheManager = cacheManagerBuilder.build(true);
    }

    public static Cache<String, String> getCacheSpace(CacheAlliance cache) {
        if (persistentCacheManager == null)
            setupCache();
        return persistentCacheManager.getCache(cache.getCacheName(), String.class, String.class);
    }

    public void putCache(String primaryKey, JsonResultResponse result, Class classView, CacheAlliance cacheSpace) throws JsonProcessingException {
        if (persistentCacheManager == null)
            setupCache();
        Cache<String, String> cache = AllianceCacheManager.getCacheSpace(cacheSpace);
        String value = mapper.writerWithView(classView)
                .writeValueAsString(result);
        cache.put(primaryKey, value);
    }

    public List<T> getResultListWeb(String entityID, Class classView, Class<? extends JsonResultResponse> clazz, CacheAlliance cacheSpace) {
        return getResultListGeneric(entityID, classView, clazz, cacheSpace, true);
    }

    public List<T> getResultList(String entityID, Class classView, Class<? extends JsonResultResponse> clazz, CacheAlliance cacheSpace) {
        return getResultListGeneric(entityID, classView, clazz, cacheSpace, false);
    }

    private List<T> getResultListGeneric(String entityID, Class classView, Class<? extends JsonResultResponse> clazz, CacheAlliance cacheSpace, boolean web) {

        if (persistentCacheManager == null) {
            if (web)
                this.web = true;
            setupCache();
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


    public static Cache<String, String> getCacheSpaceWeb(CacheAlliance cache) {
        if (persistentCacheManager == null) {
            web = true;
            setupCache();
        }
        return persistentCacheManager.getCache(cache.getCacheName(), String.class, String.class);
    }

    public static void close() {
        persistentCacheManager.close();
        log.info("closing cache");
    }

}
