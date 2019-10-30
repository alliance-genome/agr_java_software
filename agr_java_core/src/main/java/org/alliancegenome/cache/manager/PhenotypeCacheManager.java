package org.alliancegenome.cache.manager;

import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.JsonResultResponsePhenotype;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;

import java.util.List;

public class PhenotypeCacheManager extends CacheManager<PhenotypeAnnotation, JsonResultResponse<PhenotypeAnnotation>> {

    public List<PhenotypeAnnotation> getPhenotypeAnnotations(String entityID, Class<?> classView) {
        return getResultList(entityID, classView, JsonResultResponsePhenotype.class, CacheAlliance.GENE_PHENOTYPE);
    }

}
