package org.alliancegenome.cache;

import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.JsonResultResponseAllele;
import org.alliancegenome.core.service.JsonResultResponseInteraction;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.InteractionGeneJoin;

import java.util.List;

public class InteractionAllianceCacheManager extends AllianceCacheManager<InteractionGeneJoin, JsonResultResponse<InteractionGeneJoin>> {

    public List<InteractionGeneJoin> getInteractions(String entityID, Class classView) {
        return getResultList(entityID, classView, JsonResultResponseInteraction.class, CacheAlliance.INTERACTION);
    }

}
