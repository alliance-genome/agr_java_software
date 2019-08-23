package org.alliancegenome.cache;

import java.util.List;

import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.JsonResultResponseOrthology;
import org.alliancegenome.neo4j.view.OrthologView;

public class OrthologyAllianceCacheManager extends AllianceCacheManager<OrthologView, JsonResultResponse<OrthologView>> {

    public List<OrthologView> getOrthology(String entityID, Class<?> classView) {
        return getResultList(entityID, classView, JsonResultResponseOrthology.class, CacheAlliance.GENE_ORTHOLOGY);
    }

}
