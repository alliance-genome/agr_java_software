package org.alliancegenome.api.controller;

import java.util.Map;

import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.api.entity.CacheSummary;
import org.alliancegenome.api.rest.interfaces.DevtoolRESTInterface;
import org.alliancegenome.api.service.CacheStatusService;
import org.alliancegenome.cache.CacheAlliance;

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
    public CacheStatus getCacheStatusPerSpace(String cacheSpace,
                                              String entityID) {
        return service.getCacheStatus(CacheAlliance.getTypeByName(cacheSpace), entityID);
    }
}
