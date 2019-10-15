package org.alliancegenome.cache.manager;

import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.core.service.JsonResultReponsePrimaryAnnotatedEntity;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.JsonResultResponseAllele;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.node.Allele;

import java.util.List;

public class ModelAllianceCacheManager extends CacheManager<PrimaryAnnotatedEntity, JsonResultResponse<PrimaryAnnotatedEntity>> {

    public List<PrimaryAnnotatedEntity> getModels(String entityID, Class<?> classView) {
        return getResultList(entityID, classView, JsonResultReponsePrimaryAnnotatedEntity.class, CacheAlliance.ALLELE);
    }

}
