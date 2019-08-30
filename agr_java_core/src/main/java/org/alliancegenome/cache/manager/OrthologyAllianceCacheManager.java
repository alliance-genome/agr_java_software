package org.alliancegenome.cache.manager;

import java.util.List;

import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.JsonResultResponseOrthology;
import org.alliancegenome.neo4j.view.OrthologView;

public class OrthologyAllianceCacheManager extends CacheManager<OrthologView, JsonResultResponse<OrthologView>> {

    public List<OrthologView> getOrthology(String entityID, Class<?> classView) {
        return getResultList(entityID, classView, JsonResultResponseOrthology.class, CacheAlliance.GENE_ORTHOLOGY);
    }

}
