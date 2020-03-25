package org.alliancegenome.cache;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.core.application.CacheProvider;
import org.alliancegenome.neo4j.view.View;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.client.hotrod.RemoteCacheManager;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.type.CollectionType;

import lombok.extern.log4j.Log4j2;

@Log4j2
@ApplicationScoped
public class CacheService {
    
    @Inject
    private RemoteCacheManager manager;

    private CacheProvider cacheProvider;
    
    public static ObjectMapper mapper = new ObjectMapper();
    
    private CacheService() {}
    
    // Only the cacher should call this constructor
    public CacheService(CacheProvider cacheProvider) {
        this.cacheProvider = cacheProvider;
        this.manager = cacheProvider.defaultRemoteCacheManager();
    }

    static {
        mapper.disable(MapperFeature.DEFAULT_VIEW_INCLUSION);
        //mapper.configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
    }

    public List<String> getAllKeys(CacheAlliance cacheSpace) {
        RemoteCache<String, String> cache = getCacheSpace(cacheSpace);
        //System.out.println(cache);
        return new ArrayList<>(cache.keySet());
    }

    private RemoteCache<String, String> getCacheSpace(CacheAlliance cache) {
        //log.info("Getting Cache Space: " + cache.getCacheName());
        RemoteCache<String, String> remoteCache = manager.getCache(cache.getCacheName());
        
        if(remoteCache == null) {
            return cacheProvider.createCache(cache);
        }
        
        return remoteCache;
    }

    public <O> List<O> getCacheEntries(String entityID, CacheAlliance cacheSpace, Class<O> clazz) {
        CollectionType javaType = mapper.getTypeFactory()
                .constructCollectionType(List.class, clazz);

        String json = getCacheSpace(cacheSpace).get(entityID);
        if (json == null)
            return null;

        List<O> list;

        try {
            list = new ArrayList<>(mapper.readValue(json, javaType));
        } catch (IOException e) {
            log.error("Error during deserialization ", e);
            throw new RuntimeException(e);
        }

        return list;
    }

    public CacheStatus getCacheStatus(String entityName, CacheAlliance cacheSpace) {
        String json = getCacheSpace(cacheSpace).get(entityName);
        if (json == null)
            return null;

        try {
            return mapper.readerWithView(View.Cacher.class).forType(CacheStatus.class).readValue(json);
        } catch (IOException e) {
            log.error("Error during deserialization ", e);
            throw new RuntimeException(e);
        }
    }

    public <O> O getCacheEntry(String entityId, CacheAlliance cacheSpace, Class<O> clazz) {

        String json = getCacheSpace(cacheSpace).get(entityId);
        if (json == null)
            return null;

        try {
            return mapper.readValue(json, clazz);
        } catch (IOException e) {
            log.error("Error during deserialization ", e);
            throw new RuntimeException(e);
        }

    }

    public void putCacheEntry(String primaryKey, List items, Class<?> classView, CacheAlliance cacheAlliance) {
        RemoteCache<String, String> cache = manager.getCache(cacheAlliance.getCacheName());
        String value;
        try {
            value = mapper.writerWithView(classView).writeValueAsString(items);
            cache.put(primaryKey, value);
        } catch (JsonProcessingException e) {
            log.error("error while saving entry into cache", e);
            throw new RuntimeException(e);
        }
    }

    public void putCacheEntry(String primaryKey, Object object, Class<?> classView, CacheAlliance cacheAlliance) {
        RemoteCache<String, String> cache = manager.getCache(cacheAlliance.getCacheName());
        String value;
        try {
            value = mapper.writerWithView(classView).writeValueAsString(object);
            cache.put(primaryKey, value);
        } catch (JsonProcessingException e) {
            log.error("error while saving entry into cache", e);
            throw new RuntimeException(e);
        }
    }
    
}
