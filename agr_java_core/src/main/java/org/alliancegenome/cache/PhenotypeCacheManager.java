package org.alliancegenome.cache;

import java.util.List;

import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.JsonResultResponsePhenotype;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;

public class PhenotypeCacheManager extends AllianceCacheManager<PhenotypeAnnotation, JsonResultResponse<PhenotypeAnnotation>> {

    public List<PhenotypeAnnotation> getPhenotypeAnnotations(String entityID, Class<?> classView) {
        return getResultList(entityID, classView, JsonResultResponsePhenotype.class, CacheAlliance.GENE_PHENOTYPE);
    }

}
