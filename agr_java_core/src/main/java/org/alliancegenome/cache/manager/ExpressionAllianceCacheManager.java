package org.alliancegenome.cache.manager;

import java.util.List;

import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.core.ExpressionDetail;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.JsonResultResponseExpression;

public class ExpressionAllianceCacheManager extends CacheManager<ExpressionDetail, JsonResultResponse<ExpressionDetail>> {

    public List<ExpressionDetail> getExpressions(String entityID, Class<?> classView) {
        return getResultList(entityID, classView, JsonResultResponseExpression.class, CacheAlliance.GENE_EXPRESSION);
    }

}
