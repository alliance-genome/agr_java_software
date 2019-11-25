package org.alliancegenome.api.service;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.manager.BasicCachingManager;

import javax.enterprise.context.RequestScoped;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

@RequestScoped
@Log4j2
public class CacheStatusService {

    public CacheStatus getCacheStatus(CacheAlliance type) {
        return getCacheStatus(type, null);
    }

    public CacheStatus getCacheStatus(CacheAlliance type, String entityID) {
        BasicCachingManager<CacheStatus> basicManager = new BasicCachingManager<>(CacheStatus.class);
        final CacheStatus entityCache = basicManager.getEntityCache(type.getCacheName(), CacheAlliance.CACHING_STATS);
        if (entityID != null)
            entityCache.getEntityStats().keySet().removeIf(id -> !id.contains(entityID));
        return entityCache;
    }


    public Map<CacheAlliance, CacheStatus> getAllCachStatusRecords() {
        Map<CacheAlliance, CacheStatus> map = new HashMap<>();
        Arrays.stream(CacheAlliance.values()).forEach(cacheAlliance -> {
            CacheStatus status = null;
            try {
                status = getCacheStatus(cacheAlliance);
                if (status != null)
                    map.put(cacheAlliance, status);
            } catch (Exception e) {
                log.info("No suitable cache status found for " + cacheAlliance.getCacheName());
            }
        });
        return map;
    }
}
