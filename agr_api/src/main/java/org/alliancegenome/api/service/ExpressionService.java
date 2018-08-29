package org.alliancegenome.api.service;

import org.alliancegenome.api.service.helper.ExpressionDetail;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.neo4j.entity.node.BioEntityGeneExpressionJoin;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ExpressionService {

    public List<ExpressionDetail> getExpressionDetails(List<BioEntityGeneExpressionJoin> joins) {
        if (joins == null)
            joins = new ArrayList<>();
        return  joins.stream()
                .map(expressionJoin -> {
                    ExpressionDetail detail = new ExpressionDetail();
                    detail.setGene(expressionJoin.getGene());
                    detail.setStage(expressionJoin.getStage());
                    detail.setPhenotype(expressionJoin.getEntity().getWhereExpressedStatement());
                    detail.setAssay(expressionJoin.getAssay());
                    detail.setPublication(expressionJoin.getPublication());
                    return detail;
                })
                .collect(Collectors.toList());
    }
}
