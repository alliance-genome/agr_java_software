package org.alliancegenome.cache;

import org.alliancegenome.core.ExpressionDetail;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.JsonResultResponseExpression;

import java.util.List;

public class ExpressionAllianceCacheManager extends AllianceCacheManager<ExpressionDetail, JsonResultResponse<ExpressionDetail>> {

    public List<ExpressionDetail> getExpressions(String entityID, Class classView) {
        return getResultList(entityID, classView, JsonResultResponseExpression.class, CacheAlliance.EXPRESSION);
    }

}
