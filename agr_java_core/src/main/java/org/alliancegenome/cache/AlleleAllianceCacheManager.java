package org.alliancegenome.cache;

import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.JsonResultResponseAllele;
import org.alliancegenome.neo4j.entity.node.Allele;

import java.util.List;

public class AlleleAllianceCacheManager extends AllianceCacheManager<Allele, JsonResultResponse<Allele>> {

    public List<Allele> getAlleles(String entityID, Class classView) {
        return getResultList(entityID, classView, JsonResultResponseAllele.class, CacheAlliance.ALLELE);
    }

    public List<Allele> getAllelesWeb(String entityID, Class classView) {
        return getResultListWeb(entityID, classView, JsonResultResponseAllele.class, CacheAlliance.ALLELE);
    }

}
