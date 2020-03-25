package org.alliancegenome.api.controller;

import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.api.entity.CacheSummary;
import org.alliancegenome.api.rest.interfaces.DevtoolRESTInterface;
import org.alliancegenome.api.service.CacheStatusService;
import org.alliancegenome.cache.CacheAlliance;

import lombok.extern.log4j.Log4j2;

@Log4j2
@RequestScoped
public class DevtoolController implements DevtoolRESTInterface {

    @Inject
    private CacheStatusService service;

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
