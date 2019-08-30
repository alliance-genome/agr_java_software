package org.alliancegenome.cache.manager;

import java.util.List;

import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.JsonResultResponseInteraction;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;

public class InteractionAllianceCacheManager extends CacheManager<InteractionGeneJoin, JsonResultResponse<InteractionGeneJoin>> {

    public List<InteractionGeneJoin> getInteractions(String entityID, Class<?> classView) {
        return getResultList(entityID, classView, JsonResultResponseInteraction.class, CacheAlliance.GENE_INTERACTION);
    }

}
