package org.alliancegenome.api.service;

import org.alliancegenome.api.service.helper.ExpressionDetail;
import org.alliancegenome.api.service.helper.ExpressionSummary;
import org.alliancegenome.api.service.helper.ExpressionSummaryGroup;
import org.alliancegenome.api.service.helper.ExpressionSummaryGroupTerm;
import org.alliancegenome.neo4j.entity.node.BioEntityGeneExpressionJoin;
import org.alliancegenome.neo4j.repository.GeneRepository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class ExpressionService {

    public List<ExpressionDetail> getExpressionDetails(List<BioEntityGeneExpressionJoin> joins) {
        if (joins == null)
            joins = new ArrayList<>();
        return joins.stream()
                .map(expressionJoin -> {
                    ExpressionDetail detail = new ExpressionDetail();
                    detail.setGene(expressionJoin.getGene());
                    detail.setStage(expressionJoin.getStage());
                    detail.setTermName(expressionJoin.getEntity().getWhereExpressedStatement());
                    detail.setAssay(expressionJoin.getAssay());
                    detail.setPublication(expressionJoin.getPublication());
                    return detail;
                })
                .collect(Collectors.toList());
    }

    public ExpressionSummary getExpressionSummary(List<BioEntityGeneExpressionJoin> joins) {
        if (joins == null)
            joins = new ArrayList<>();
        ExpressionSummary summary = new ExpressionSummary();
        summary.setTotalAnnotations(joins.size());

        // calculate GO_CC set
        // get Parent terms
        GeneRepository repository = new GeneRepository();
        Map<String, List<BioEntityGeneExpressionJoin>> parentTermMap = new HashMap<>();

        for (BioEntityGeneExpressionJoin join : joins) {
            List<String> goParentTerms = repository.getGOParentTerms(join.getEntity());
            for (String parent : goParentTerms) {
                if (parentTermMap.get(parent) == null) {
                    List<BioEntityGeneExpressionJoin> list = new ArrayList<>();
                    list.add(join);
                    parentTermMap.put(parent, list);
                } else {
                    List<BioEntityGeneExpressionJoin> joinList = parentTermMap.get(parent);
                    joinList.add(join);
                }
            }
        }
        ExpressionSummaryGroup group = new ExpressionSummaryGroup();
        summary.addGroup(group);
        group.setName("GO");
        repository.getGoCCSlimList().forEach((goId, goName) ->
        {
            ExpressionSummaryGroupTerm term = new ExpressionSummaryGroupTerm();
            term.setId(goId);
            term.setName(goName);
            if (parentTermMap.get(goId) != null) {
                term.setNumberOfAnnotations(parentTermMap.get(goId).size());
            }
            group.addGroupTerm(term);
        });
        return summary;
    }
}
