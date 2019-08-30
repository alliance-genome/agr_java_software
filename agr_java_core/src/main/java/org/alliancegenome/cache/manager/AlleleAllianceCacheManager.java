package org.alliancegenome.cache.manager;

import java.util.List;

import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.JsonResultResponseAllele;
import org.alliancegenome.neo4j.entity.node.Allele;

public class AlleleAllianceCacheManager extends CacheManager<Allele, JsonResultResponse<Allele>> {

    public List<Allele> getAlleles(String entityID, Class<?> classView) {
        return getResultList(entityID, classView, JsonResultResponseAllele.class, CacheAlliance.ALLELE);
    }

}
