package org.alliancegenome.cacher.cachers;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.log4j.Log4j2;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.manager.ExpressionAllianceCacheManager;
import org.alliancegenome.core.ExpressionDetail;
import org.alliancegenome.core.service.JsonResultResponseExpression;
import org.alliancegenome.neo4j.entity.node.BioEntityGeneExpressionJoin;
import org.alliancegenome.neo4j.entity.node.GOTerm;
import org.alliancegenome.neo4j.entity.node.UBERONTerm;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.View;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Log4j2
public class ExpressionCacher extends Cacher {

    private static List<String> parentTermIDs = new ArrayList<>();

    static {
        // anatomical entity
        parentTermIDs.add("UBERON:0001062");
        // processual entity stage
        parentTermIDs.add("UBERON:0000000");
        // cellular Component
        parentTermIDs.add("GO:0005575");
    }

    @Override
    protected void cache() {

        GeneRepository geneRepository = new GeneRepository();

        startProcess("geneRepository.getAllExpressionAnnotations");

        List<BioEntityGeneExpressionJoin> joins = geneRepository.getAllExpressionAnnotations();

        finishProcess();

        startProcess("allExpression", joins.size());

        List<ExpressionDetail> allExpression = joins.stream()
                .map(expressionJoin -> {
                    ExpressionDetail detail = new ExpressionDetail();
                    detail.setGene(expressionJoin.getGene());
                    detail.setTermName(expressionJoin.getEntity().getWhereExpressedStatement());
                    detail.setAssay(expressionJoin.getAssay());
                    detail.setDataProvider(expressionJoin.getGene().getDataProvider());
                    if (expressionJoin.getStage() != null)
                        detail.setStage(expressionJoin.getStage());
                    detail.setPublications(new TreeSet<>(expressionJoin.getPublications()));
                    // Remove this check in future checkins.
                    if (expressionJoin.getCrossReferences() != null) {
                        if (expressionJoin.getCrossReferences().get(0).getName() == null)
                            log.info("CrossRef: " + expressionJoin.getCrossReferences().get(0));
                    }
                    detail.setCrossReferences(expressionJoin.getCrossReferences());
                    // add AO terms and All AO parent term
                    List<String> aoList = expressionJoin.getEntity().getAoTermList().stream().map(UBERONTerm::getPrimaryKey).collect(Collectors.toList());

                    Set<String> parentTermIDs = getParentTermIDs(aoList);
                    if (parentTermIDs != null)
                        aoList.addAll(parentTermIDs);
                    detail.addTermIDs(aoList);

                    // add GO terms and All-GO parent term
                    List<String> goList = expressionJoin.getEntity().getCcRibbonTermList().stream().map(GOTerm::getPrimaryKey).collect(Collectors.toList());
                    Set<String> goParentTerms = getGOParentTermIDs(goList);
                    if (goParentTerms != null) {
                        goList.addAll(goParentTerms);
                    }
                    detail.addTermIDs(goList);
                    if (expressionJoin.getStageTerm() != null) {
                        String stageID = expressionJoin.getStageTerm().getPrimaryKey();
                        detail.addTermID(stageID);
                        detail.addTermIDs(getParentTermIDs(Collections.singletonList(stageID)));
                    }
                    progressProcess();
                    return detail;
                })
                .collect(Collectors.toList());

        finishProcess();

        startProcess("geneExpressionMap", allExpression.size());

        Map<String, List<ExpressionDetail>> geneExpressionMap = allExpression.stream()
                .collect(groupingBy(expressionDetail -> expressionDetail.getGene().getPrimaryKey()));

        finishProcess();

        ExpressionAllianceCacheManager manager = new ExpressionAllianceCacheManager();

        startProcess("geneExpressionMap into Cache", geneExpressionMap.size());

        geneExpressionMap.forEach((key, value) -> {
            JsonResultResponseExpression result = new JsonResultResponseExpression();
            result.setResults(value);
            try {
                manager.putCache(key, result, View.Expression.class, CacheAlliance.GENE_EXPRESSION);
                progressProcess();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

        });
        finishProcess();
        setCacheStatus(joins.size(), CacheAlliance.GENE_EXPRESSION.getCacheName());

        geneRepository.clearCache();

    }

    private Set<String> getParentTermIDs(List<String> idList) {
        if (idList == null || idList.isEmpty())
            return null;
        DiseaseRepository repository = new DiseaseRepository();
        Set<String> parentSet = new HashSet<>(4);
        Map<String, Set<String>> map = repository.getClosureMappingUberon();
        idList.forEach(id -> {
            parentTermIDs.forEach(parentTermID -> {
                if (map.get(parentTermID) != null && map.get(parentTermID).contains(id))
                    parentSet.add(parentTermID);
            });
            if (id.equals("UBERON:AnatomyOtherLocation"))
                parentSet.add(parentTermIDs.get(0));
            if (id.equals("UBERON:PostEmbryonicPreAdult"))
                parentSet.add(parentTermIDs.get(1));
        });
        return parentSet;
    }

    private Set<String> getGOParentTermIDs(List<String> goList) {
        if (goList == null || goList.isEmpty())
            return null;
        DiseaseRepository repository = new DiseaseRepository();
        Set<String> parentSet = new HashSet<>(4);
        Map<String, Set<String>> map = repository.getClosureMappingGO();
        goList.forEach(id -> {
            parentTermIDs.forEach(parentTermID -> {
                if (map.get(parentTermID) != null && map.get(parentTermID).contains(id))
                    parentSet.add(parentTermID);
            });
            if (id.equals("GO:otherLocations"))
                parentSet.add(parentTermIDs.get(2));
        });
        return parentSet;
    }
}
