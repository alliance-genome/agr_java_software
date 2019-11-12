package org.alliancegenome.cache.manager;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.core.service.JsonResultResponse;
import org.infinispan.client.hotrod.RemoteCache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Log4j2
public class CacheManager<T, U extends JsonResultResponse<T>> extends BasicCacheManager<T> {

    private static RemoteCache<String, String> getCacheSpace(CacheAlliance cache) {
        //log.info("Getting Cache Space: " + cache.getCacheName());
        return rmc.getCache(cache.getCacheName());
    }

    public void putCache(String primaryKey, JsonResultResponse<T> result, Class<?> classView, CacheAlliance cacheSpace) throws JsonProcessingException {
        RemoteCache<String, String> cache = getCacheSpace(cacheSpace);
        String value = mapper.writerWithView(classView).writeValueAsString(result);

        cache.put(primaryKey, value);
    }

    public List<T> getResultList(String entityID, Class<?> classView, Class<? extends JsonResultResponse<T>> clazz, CacheAlliance cacheSpace) {
        return getResultListGeneric(entityID, classView, clazz, cacheSpace);
    }

    public static CacheStatus getCacheStatus(CacheAlliance cacheSpace) {

        CacheStatus status = new CacheStatus(cacheSpace.getCacheName());

        RemoteCache<String, String> cache = getCacheSpace(cacheSpace);

        status.setNumberOfEntities(cache.size());

        log.info("Cache Status: " + status);

        return status;
    }

    private List<T> getResultListGeneric(String entityID, Class<?> classView, Class<? extends JsonResultResponse<T>> clazz, CacheAlliance cacheSpace) {

        String json = getCacheSpace(cacheSpace).get(entityID);
        if (json == null)
            return new ArrayList<>();
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
