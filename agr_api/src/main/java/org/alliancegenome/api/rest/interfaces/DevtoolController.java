package org.alliancegenome.api.rest.interfaces;

import org.alliancegenome.api.entity.CacheSummary;
import org.alliancegenome.api.repository.DiseaseCacheRepository;
import org.alliancegenome.neo4j.repository.*;

public class DevtoolController implements DevtoolRESTInterface {

    @Override
    public CacheSummary getCacheStatus() {
        CacheSummary summary = new CacheSummary();

        DiseaseCacheRepository cacheRepository = new DiseaseCacheRepository();
        InteractionCacheRepository interactionCacheRepository = new InteractionCacheRepository();
        ExpressionCacheRepository expressionCacheRepository = new ExpressionCacheRepository();
        PhenotypeCacheRepository phenotypeCacheRepository = new PhenotypeCacheRepository();
        GeneCacheRepository geneCacheRepository = new GeneCacheRepository();
        AlleleCacheRepository alleleCacheRepository = new AlleleCacheRepository();

        summary.addCacheStatus(cacheRepository.getCacheStatus());
        summary.addCacheStatus(interactionCacheRepository.getCacheStatus());
        summary.addCacheStatus(expressionCacheRepository.getCacheStatus());
        summary.addCacheStatus(phenotypeCacheRepository.getCacheStatus());
        summary.addCacheStatus(geneCacheRepository.getCacheStatus());
        summary.addCacheStatus(alleleCacheRepository.getCacheStatus());

        return summary;
    }
}
