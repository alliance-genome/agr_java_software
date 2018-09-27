package org.alliancegenome.api.service;

import org.alliancegenome.api.service.helper.ExpressionDetail;
import org.alliancegenome.api.service.helper.ExpressionSummary;
import org.alliancegenome.api.service.helper.ExpressionSummaryGroup;
import org.alliancegenome.api.service.helper.ExpressionSummaryGroupTerm;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

public class ExpressionService {

    public JsonResultResponse<ExpressionDetail> getExpressionDetails(List<BioEntityGeneExpressionJoin> joins, Pagination pagination) {
        // grouping by: gene, term name, ribbon stage, assay
        // to collate publications / sources
        Map<Gene, Map<String, Map<Optional<UBERONTerm>, Map<MMOTerm, Set<BioEntityGeneExpressionJoin>>>>> groupedRecords = joins.stream()
                .collect(groupingBy(BioEntityGeneExpressionJoin::getGene, groupingBy(join -> join.getEntity().getWhereExpressedStatement(),
                        groupingBy(join -> Optional.ofNullable(join.getStageTerm()), groupingBy(BioEntityGeneExpressionJoin::getAssay, toSet())))));

        List<ExpressionDetail> expressionDetails = new ArrayList<>();
        groupedRecords.forEach((gene, termNameMap) -> {
            termNameMap.forEach((termName, stageMap) -> {
                stageMap.forEach((stage, assayMap) -> {
                    assayMap.forEach((assay, bioJoins) -> {
                        ExpressionDetail detail = new ExpressionDetail();
                        detail.setGene(gene);
                        detail.setTermName(termName);
                        detail.setAssay(assay);
                        detail.setDataProvider(gene.getDataProvider());
                        stage.ifPresent(detail::setStage);
                        detail.setPublications(bioJoins.stream().map(BioEntityGeneExpressionJoin::getPublication).collect(Collectors.toList()));
                        expressionDetails.add(detail);
                    });
                });
            });
        });

        // sorting
        HashMap<FieldFilter, Comparator<ExpressionDetail>> sortingMapping = new LinkedHashMap<>();
        sortingMapping.put(FieldFilter.FSPECIES, Comparator.comparing(o -> o.getGene().getTaxonId().toUpperCase()));
        sortingMapping.put(FieldFilter.GENE_NAME, Comparator.comparing(o -> o.getGene().getSymbol().toUpperCase()));
        //sortingMapping.put(FieldFilter.TERM_NAME, Comparator.comparing(o -> o.getTerm().toUpperCase()));
//        sortingMapping.put(FieldFilter.STAGE, Comparator.comparing(o -> o.getStage().getPrimaryKey().toUpperCase()));
        sortingMapping.put(FieldFilter.ASSAY, Comparator.comparing(o -> o.getAssay().getName().toUpperCase()));

        Comparator<ExpressionDetail> comparator = null;
        FieldFilter sortByField = pagination.getSortByField();
        if (sortByField != null) {
            comparator = sortingMapping.get(sortByField);
/*
            if (!pagination.getAsc())
                comparator.reversed();
*/
        }
        if (comparator != null)
            expressionDetails.sort(comparator);
        for (FieldFilter fieldFilter : sortingMapping.keySet()) {
            if (sortByField != null && sortByField.equals(fieldFilter)) {
                continue;
            }
            Comparator<ExpressionDetail> comp = sortingMapping.get(fieldFilter);
            if (comparator == null)
                comparator = comp;
            else
                comparator = comparator.thenComparing(comp);
        }
        expressionDetails.sort(comparator);
        JsonResultResponse<ExpressionDetail> response = new JsonResultResponse<>();
        response.setTotal(expressionDetails.size());
        // pagination
        List<ExpressionDetail> paginatedJoinList;
        if (pagination.isCount()) {
            paginatedJoinList = expressionDetails;
        } else {
            paginatedJoinList = expressionDetails.stream()
                    .skip(pagination.getStart())
                    .limit(pagination.getLimit())
                    .collect(Collectors.toList());
        }

        if (paginatedJoinList == null)
            paginatedJoinList = new ArrayList<>();
        response.setResults(paginatedJoinList);
        return response;
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
                .filter(join -> join.getStageTerm() != null)
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
