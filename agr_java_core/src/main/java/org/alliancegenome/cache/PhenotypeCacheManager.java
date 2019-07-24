package org.alliancegenome.cache;

import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.JsonResultResponsePhenotype;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;

import java.util.List;

public class PhenotypeCacheManager extends AllianceCacheManager<PhenotypeAnnotation, JsonResultResponse<PhenotypeAnnotation>> {

    public List<PhenotypeAnnotation> getPhenotypeAnnotations(String entityID, Class classView) {
        return getResultList(entityID, classView, JsonResultResponsePhenotype.class, CacheAlliance.PHENOTYPE);
    }


    public List<PhenotypeAnnotation> getPhenotypeAnnotationsWeb(String entityID, Class classView) {
        return getResultListWeb(entityID, classView, JsonResultResponsePhenotype.class, CacheAlliance.PHENOTYPE);
    }

}
