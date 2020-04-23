package org.alliancegenome.api.service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.CacheService;

import lombok.extern.log4j.Log4j2;

@Log4j2
@RequestScoped
public class CacheStatusService {

    @Inject
    private CacheService cacherService;
    
    public CacheStatus getCacheStatus(CacheAlliance type) {
        return getCacheStatus(type, null);
    }

    public CacheStatus getCacheStatus(CacheAlliance type, String entityID) {

        final CacheStatus entityCache = cacherService.getCacheEntry(type.getCacheName(), type, CacheStatus.class);
        if (entityID != null)
            entityCache.getEntityStats().keySet().removeIf(id -> !id.contains(entityID));
        return entityCache;
    }

    public Map<CacheAlliance, CacheStatus> getAllCachStatusRecords() {
        Map<CacheAlliance, CacheStatus> map = new HashMap<>();
        Arrays.stream(CacheAlliance.values()).forEach(cacheAlliance -> {
            CacheStatus status;
            try {
                status = getCacheStatus(cacheAlliance, null);
                if (status != null)
                    map.put(cacheAlliance, status);
            } catch (Exception e) {
                log.info("No suitable cache status found for " + cacheAlliance.getCacheName());
            }
        });
        return map;
    }

    public String getCacheObject(String id, String cacheName) {
        CacheAlliance cache = CacheAlliance.getTypeByName(cacheName);
        if (cache == null)
            return "No Cache with name " + cacheName + " found";
        return cacherService.getCacheEntry(id, cache, String.class);
    }
}
