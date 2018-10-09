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

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toSet;

public class ExpressionService {

    public JsonResultResponse<ExpressionDetail> getExpressionDetails(List<BioEntityGeneExpressionJoin> joins, Pagination pagination) {
        Map<Gene, Map<ExpressionBioEntity, Map<Optional<Stage>, Map<MMOTerm, Set<BioEntityGeneExpressionJoin>>>>> groupedRecords = getGeneTermStageAssayMap(joins);

        List<ExpressionDetail> expressionDetails = new ArrayList<>();
        groupedRecords.forEach((gene, termNameMap) -> {
            termNameMap.forEach((entity, stageMap) -> {
                stageMap.forEach((stage, assayMap) -> {
                    assayMap.forEach((assay, bioJoins) -> {
                        ExpressionDetail detail = new ExpressionDetail();
                        detail.setGene(gene);
                        detail.setTermName(entity.getWhereExpressedStatement());
                        detail.setAssay(assay);
                        detail.setDataProvider(gene.getDataProvider());
                        stage.ifPresent(detail::setStage);
                        detail.setPublications(bioJoins.stream().map(BioEntityGeneExpressionJoin::getPublication).collect(Collectors.toList()));
                        Optional<BioEntityGeneExpressionJoin> join = bioJoins.stream().findFirst();
                        join.ifPresent(bioEntityGeneExpressionJoin -> detail.setCrossReference(bioEntityGeneExpressionJoin.getCrossReference()));
                        expressionDetails.add(detail);
                    });
                });
            });
        });

        // sorting
        HashMap<FieldFilter, Comparator<ExpressionDetail>> sortingMapping = new LinkedHashMap<>();
        sortingMapping.put(FieldFilter.FSPECIES, Comparator.comparing(o -> o.getGene().getTaxonId().toUpperCase()));
        sortingMapping.put(FieldFilter.GENE_NAME, Comparator.comparing(o -> o.getGene().getSymbol().toUpperCase()));
        sortingMapping.put(FieldFilter.TERM_NAME, Comparator.comparing(o -> o.getTermName().toUpperCase()));
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

    private Map<Gene, Map<ExpressionBioEntity, Map<Optional<Stage>, Map<MMOTerm, Set<BioEntityGeneExpressionJoin>>>>> getGeneTermStageAssayMap(List<BioEntityGeneExpressionJoin> joins) {
        // grouping by: gene, term name, ribbon stage, assay
        // to collate publications / sources
        return joins.stream()
                .collect(groupingBy(BioEntityGeneExpressionJoin::getGene, groupingBy(BioEntityGeneExpressionJoin::getEntity,
                        groupingBy(join -> Optional.ofNullable(join.getStage()), groupingBy(BioEntityGeneExpressionJoin::getAssay, toSet())))));
    }

    private Map<Gene, Map<ExpressionBioEntity, Map<Optional<UBERONTerm>, Map<MMOTerm, Set<BioEntityGeneExpressionJoin>>>>> getGeneTermStageRibbonAssayMap(List<BioEntityGeneExpressionJoin> joins) {
        // grouping by: gene, term name, ribbon stage, assay
        // to collate publications / sources
        return joins.stream()
                .collect(groupingBy(BioEntityGeneExpressionJoin::getGene, groupingBy(BioEntityGeneExpressionJoin::getEntity,
                        groupingBy(join -> Optional.ofNullable(join.getStageTerm()), groupingBy(BioEntityGeneExpressionJoin::getAssay, toSet())))));
    }

    public ExpressionSummary getExpressionSummary(List<BioEntityGeneExpressionJoin> joins) {
        if (joins == null)
            joins = new ArrayList<>();
        // group together records where only publication is different and treat them as a single record
        Map<Gene, Map<ExpressionBioEntity, Map<Optional<UBERONTerm>, Map<MMOTerm, Set<BioEntityGeneExpressionJoin>>>>> groupedRecords = getGeneTermStageRibbonAssayMap(joins);

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
        // list of all terms over grouped list
        List<UBERONTerm> aoGroupedList = new ArrayList<>();
        groupedRecords.forEach((gene, termNameMap) -> {
            termNameMap.forEach((entity, stageMap) -> {
                stageMap.forEach((stage, assayMap) -> {
                    assayMap.forEach((assay, bioJoins) -> {
                        aoGroupedList.addAll(entity.getAoTermList());
                    });
                });
            });
        });

        // create histogram from list
        Map<String, Long> aoHistogram = aoGroupedList.stream()
                .collect(Collectors.groupingBy(UBERONTerm::getPrimaryKey, Collectors.counting()));

        ExpressionSummaryGroup aoGroup = populateGroupInfo("Anatomy", aoHistogram, repository.getFullAoList());
        summary.addGroup(aoGroup);

        int sumAO = aoGroup.getTerms().stream().mapToInt(ExpressionSummaryGroupTerm::getNumberOfAnnotations).sum();
        aoGroup.setTotalAnnotations(sumAO);

        // create Stage histogram
        // list of all terms over grouped list
        List<UBERONTerm> stageGroupedList = new ArrayList<>();
        groupedRecords.forEach((gene, termNameMap) -> {
            termNameMap.forEach((entity, stageMap) -> {
                stageMap.forEach((stage, assayMap) -> {
                    assayMap.forEach((assay, bioJoins) -> {
                        stage.ifPresent(stageGroupedList::add);
                    });
                });
            });
        });
        Map<String, Long> stageHistogram = stageGroupedList.stream()
                .collect(Collectors.groupingBy(UBERONTerm::getPrimaryKey, Collectors.counting()));

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

    public String getTextFile(JsonResultResponse<ExpressionDetail> result) {
        StringBuilder builder = new StringBuilder();
        StringJoiner headerJoiner = new StringJoiner("\t");
        headerJoiner.add("Species");
        headerJoiner.add("Gene Symbol");
        headerJoiner.add("Term");
        headerJoiner.add("Stage");
        headerJoiner.add("Assay");
        headerJoiner.add("Reference");
        headerJoiner.add("Source");
        builder.append(headerJoiner.toString());
        builder.append(System.getProperty("line.separator"));
        result.getResults().forEach(expressionDetail -> {
            StringJoiner rowJoiner = new StringJoiner("\t");
            rowJoiner.add(expressionDetail.getGene().getSpeciesName());
            rowJoiner.add(expressionDetail.getGene().getSymbol());
            rowJoiner.add(expressionDetail.getTermName());
            rowJoiner.add(expressionDetail.getStage().getPrimaryKey());
            rowJoiner.add(expressionDetail.getAssay().getDisplay_synonym());
            rowJoiner.add(expressionDetail.getPublications().stream().map(Publication::getPubId).collect(Collectors.joining(",")));
            rowJoiner.add(expressionDetail.getDataProvider());
            builder.append(rowJoiner.toString());
            builder.append(System.getProperty("line.separator"));
        });
        return builder.toString();
    }
}
