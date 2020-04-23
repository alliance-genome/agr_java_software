package org.alliancegenome.api.controller;

import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.api.entity.CacheSummary;
import org.alliancegenome.api.rest.interfaces.CacheRESTInterface;
import org.alliancegenome.api.service.CacheStatusService;
import org.alliancegenome.cache.CacheAlliance;

import io.swagger.annotations.Api;

@RequestScoped
@Path("/cache")
@Api(value = "Cache")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CacheController implements CacheRESTInterface {

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
