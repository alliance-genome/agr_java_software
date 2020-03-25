package org.alliancegenome.cacher.cachers;

import static java.util.stream.Collectors.groupingBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.core.ExpressionDetail;
import org.alliancegenome.neo4j.entity.node.BioEntityGeneExpressionJoin;
import org.alliancegenome.neo4j.entity.node.GOTerm;
import org.alliancegenome.neo4j.entity.node.Species;
import org.alliancegenome.neo4j.entity.node.UBERONTerm;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.View;

import lombok.extern.log4j.Log4j2;

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
                    detail.setUberonTermIDs(aoList);

                    Set<String> parentTermIDs = getParentTermIDs(aoList);
                    if (parentTermIDs != null)
                        aoList.addAll(parentTermIDs);
                    detail.addTermIDs(aoList);

                    // add GO terms and All-GO parent term
                    List<String> goList = expressionJoin.getEntity().getCcRibbonTermList().stream().map(GOTerm::getPrimaryKey).collect(Collectors.toList());
                    detail.setGoTermIDs(goList);
                    Set<String> goParentTerms = getGOParentTermIDs(goList);
                    if (goParentTerms != null) {
                        goList.addAll(goParentTerms);
                    }
                    detail.addTermIDs(goList);
                    if (expressionJoin.getStageTerm() != null) {
                        String stageID = expressionJoin.getStageTerm().getPrimaryKey();
                        detail.addTermID(stageID);
                        detail.setStageTermID(stageID);
                        detail.addTermIDs(getParentTermIDs(List.of(stageID)));
                    }
                    progressProcess();
                    return detail;
                })
                .collect(Collectors.toList());

        finishProcess();

        joins.clear();

        startProcess("geneExpressionMap", allExpression.size());

        Map<String, List<ExpressionDetail>> geneExpressionMap = allExpression.stream()
                .collect(groupingBy(expressionDetail -> expressionDetail.getGene().getPrimaryKey()));

        finishProcess();

        populateCacheFromMap(geneExpressionMap, View.Expression.class, CacheAlliance.GENE_EXPRESSION);

        CacheStatus status = new CacheStatus(CacheAlliance.GENE_EXPRESSION);
        status.setNumberOfEntities(allExpression.size());

        Map<String, List<Species>> speciesStats = allExpression.stream()
                .filter(expressionDetail -> expressionDetail.getGene() != null)
                .map(expressionDetail -> expressionDetail.getGene().getSpecies())
                .collect(groupingBy(Species::getName));

        Map<String, Integer> entityStats = new TreeMap<>();
        geneExpressionMap.forEach((geneID, annotations) -> entityStats.put(geneID, annotations.size()));
        populateStatisticsOnStatus(status, entityStats, speciesStats);

        status.setJsonViewClass(View.Expression.class.getSimpleName());
        status.setCollectionEntity(ExpressionDetail.class.getSimpleName());
        setCacheStatus(status);

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
