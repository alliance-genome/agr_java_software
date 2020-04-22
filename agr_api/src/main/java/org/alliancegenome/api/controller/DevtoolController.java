package org.alliancegenome.api.controller;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.api.entity.CacheSummary;
import org.alliancegenome.api.rest.interfaces.DevtoolRESTInterface;
import org.alliancegenome.api.service.CacheStatusService;
import org.alliancegenome.cache.CacheAlliance;

import java.util.Map;

@Log4j2
public class DevtoolController implements DevtoolRESTInterface {

    private CacheStatusService service = new CacheStatusService();

    @Override
    public CacheSummary getCacheStatus() {
        CacheSummary summary = new CacheSummary();
        Map<CacheAlliance, CacheStatus> map = service.getAllCachStatusRecords();
        map.forEach((name, cacheStatus) -> summary.addCacheStatus(cacheStatus));
        return summary;
    }

    @Override
    public CacheStatus getCacheStatusPerSpace(String cacheSpace) {
        return service.getCacheStatus(CacheAlliance.getTypeByName(cacheSpace));
    }

    @Override
    public String getCacheObject(String entityId, String cacheName) {
        return service.getCacheObject(entityId, cacheName);
    }
}
