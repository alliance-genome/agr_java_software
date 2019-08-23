package org.alliancegenome.api.controller;

import org.alliancegenome.api.entity.CacheSummary;
import org.alliancegenome.api.rest.interfaces.DevtoolRESTInterface;
import org.alliancegenome.cache.AllianceCacheManager;
import org.alliancegenome.cache.CacheAlliance;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class DevtoolController implements DevtoolRESTInterface {

    @Override
    public CacheSummary getCacheStatus() {
        CacheSummary summary = new CacheSummary();

        //DiseaseCacheRepository diseaseCacheRepository = new DiseaseCacheRepository();
        //InteractionCacheRepository interactionCacheRepository = new InteractionCacheRepository();
        //ExpressionCacheRepository expressionCacheRepository = new ExpressionCacheRepository();
        //PhenotypeCacheRepository phenotypeCacheRepository = new PhenotypeCacheRepository();
        //GeneCacheRepository geneCacheRepository = new GeneCacheRepository();
        //AlleleCacheRepository alleleCacheRepository = new AlleleCacheRepository();
        
        for(CacheAlliance c: CacheAlliance.values()) {
            summary.addCacheStatus(AllianceCacheManager.getCacheStatus(c));
        }
        
        //CacheStatus status = new CacheStatus(name);
        
        //summary.addCacheStatus();
        

        return summary;
    }
}
