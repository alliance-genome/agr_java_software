package org.alliancegenome.api.controller;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.api.entity.CacheSummary;
import org.alliancegenome.api.rest.interfaces.DevtoolRESTInterface;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.manager.BasicCacheManager;

import java.util.Map;

@Log4j2
public class DevtoolController implements DevtoolRESTInterface {

    @Override
    public CacheSummary getCacheStatus() {
        CacheSummary summary = new CacheSummary();

        //InteractionCacheRepository interactionCacheRepository = new InteractionCacheRepository();
        //ExpressionCacheRepository expressionCacheRepository = new ExpressionCacheRepository();
        //PhenotypeCacheRepository phenotypeCacheRepository = new PhenotypeCacheRepository();
        //GeneCacheRepository geneCacheRepository = new GeneCacheRepository();
        //AlleleCacheRepository alleleCacheRepository = new AlleleCacheRepository();

        BasicCacheManager<CacheStatus> basicManager = new BasicCacheManager<>();
        Map<String, CacheStatus> map = basicManager.getAllCacheEntries(CacheAlliance.CACHING_STATS);

        map.forEach((name, cacheStatus) -> summary.addCacheStatus(cacheStatus));
        return summary;
    }
}
