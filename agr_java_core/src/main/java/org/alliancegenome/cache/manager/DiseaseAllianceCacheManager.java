package org.alliancegenome.cache.manager;

import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.JsonResultResponseDiseaseAnnotation;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.view.View;

import java.util.List;

public class DiseaseAllianceCacheManager extends CacheManager<DiseaseAnnotation, JsonResultResponse<DiseaseAnnotation>> {

    public List<DiseaseAnnotation> getDiseaseAnnotations(String entityID, Class<?> classView) {
        return getResultList(entityID, classView, JsonResultResponseDiseaseAnnotation.class, CacheAlliance.DISEASE_ANNOTATION);
    }

    public List<DiseaseAnnotation> getDiseaseAlleleAnnotations(String diseaseID, Class<View.DiseaseCacher> classView) {
        return getResultList(diseaseID, classView, JsonResultResponseDiseaseAnnotation.class, CacheAlliance.DISEASE_ALLELE_ANNOTATION);
    }
}
