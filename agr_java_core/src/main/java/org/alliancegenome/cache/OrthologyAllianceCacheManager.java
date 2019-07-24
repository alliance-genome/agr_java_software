package org.alliancegenome.cache;

import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.JsonResultResponseOrthology;
import org.alliancegenome.neo4j.view.OrthologView;

import java.util.List;

public class OrthologyAllianceCacheManager extends AllianceCacheManager<OrthologView, JsonResultResponse<OrthologView>> {

    public List<OrthologView> getOrthology(String entityID, Class classView) {
        return getResultList(entityID, classView, JsonResultResponseOrthology.class, CacheAlliance.ORTHOLOGY);
    }

    public List<OrthologView> getOrthologyWeb(String entityID, Class classView) {
        return getResultListWeb(entityID, classView, JsonResultResponseOrthology.class, CacheAlliance.ORTHOLOGY);
    }

}
