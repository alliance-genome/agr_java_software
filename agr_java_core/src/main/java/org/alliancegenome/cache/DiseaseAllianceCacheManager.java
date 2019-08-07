package org.alliancegenome.cache;

import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.JsonResultResponseDiseaseAnnotation;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;

import java.util.List;

public class DiseaseAllianceCacheManager extends AllianceCacheManager<DiseaseAnnotation, JsonResultResponse<DiseaseAnnotation>> {

    public List<DiseaseAnnotation> getDiseaseAnnotations(String entityID, Class classView) {
        return getResultList(entityID, classView, JsonResultResponseDiseaseAnnotation.class, CacheAlliance.DISEASE_ANNOTATION);
    }

}
