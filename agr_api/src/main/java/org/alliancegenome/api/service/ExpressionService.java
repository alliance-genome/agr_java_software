package org.alliancegenome.api.service;

import org.alliancegenome.api.service.helper.ExpressionDetail;
import org.alliancegenome.api.service.helper.ExpressionSummary;
import org.alliancegenome.api.service.helper.ExpressionSummaryGroup;
import org.alliancegenome.api.service.helper.ExpressionSummaryGroupTerm;
import org.alliancegenome.neo4j.entity.node.BioEntityGeneExpressionJoin;
import org.alliancegenome.neo4j.entity.node.ExpressionBioEntity;
import org.alliancegenome.neo4j.entity.node.UBERONTerm;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.apache.commons.collections4.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
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

        // calculate GO_CC set
        // get Parent terms
        GeneRepository repository = new GeneRepository();
        Map<String, List<BioEntityGeneExpressionJoin>> parentTermMap = new HashMap<>();

        for (BioEntityGeneExpressionJoin join : joins) {
            ExpressionBioEntity entity = join.getEntity();
            if (entity.getGoTerm() != null) {
                List<String> goParentTerms = repository.getGOParentTerms(entity);
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
        }
        Map<String, Long> goHistogram = parentTermMap.keySet().stream()
                .collect(Collectors.toMap(o -> o, o -> ((long) parentTermMap.get(o).size())));
        ExpressionSummaryGroup goCcGroup = populateGroupInfo("Cellular Component", goHistogram, repository.getGoCCSlimList());
        summary.addGroup(goCcGroup);

        // create AO histogram
        // list of all terms
        List<UBERONTerm> aoList = joins.stream()
                .filter(join -> join.getEntity().getAoTermList() != null)
                .map(join -> join.getEntity().getAoTermList())
                .flatMap(List::stream)
                .collect(Collectors.toList());
        // create histogram from list
        Map<String, Long> aoHistogram = aoList.stream()
                .collect(Collectors.groupingBy(UBERONTerm::getPrimaryKey, Collectors.counting()));

        ExpressionSummaryGroup aoGroup = populateGroupInfo("Anatomy", aoHistogram, repository.getFullAoList());
        summary.addGroup(aoGroup);

        int sumAO = aoGroup.getTerms().stream().mapToInt(ExpressionSummaryGroupTerm::getNumberOfAnnotations).sum();
        aoGroup.setTotalAnnotations(sumAO);

        // create Stage histogram
        Map<String, Long> stageHistogram = joins.stream()
                .collect(Collectors.groupingBy(join -> join.getStageTerm().getPrimaryKey(), Collectors.counting()));
        ExpressionSummaryGroup stageGroup = populateGroupInfo("Stage", stageHistogram, repository.getStageList());
        summary.addGroup(stageGroup);

        int sumStage = stageGroup.getTerms().stream().mapToInt(ExpressionSummaryGroupTerm::getNumberOfAnnotations).sum();
        stageGroup.setTotalAnnotations(sumStage);

        // add all annotations per goCcGroup
        int sumGoCC = goCcGroup.getTerms().stream().mapToInt(ExpressionSummaryGroupTerm::getNumberOfAnnotations).sum();
        goCcGroup.setTotalAnnotations(sumGoCC);
        summary.setTotalAnnotations(sumGoCC + sumAO + sumStage);
        return summary;
    }

    private ExpressionSummaryGroup populateGroupInfo(String groupName, Map<String, Long> histogram, Map<String, String> entityList) {
        ExpressionSummaryGroup group = new ExpressionSummaryGroup();
        group.setName(groupName);
        entityList.forEach((id, name) ->
        {
            ExpressionSummaryGroupTerm term = new ExpressionSummaryGroupTerm();
            term.setId(id);
            term.setName(name);
            if (histogram.get(id) != null) {
                term.setNumberOfAnnotations((int) (long) histogram.get(id));
            }
            group.addGroupTerm(term);
        });
        return group;
    }
}
