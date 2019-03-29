package org.alliancegenome.api.service;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.alliancegenome.core.ExpressionDetail;
import org.alliancegenome.api.service.helper.ExpressionSummary;
import org.alliancegenome.api.service.helper.ExpressionSummaryGroup;
import org.alliancegenome.api.service.helper.ExpressionSummaryGroupTerm;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.BioEntityGeneExpressionJoin;
import org.alliancegenome.neo4j.entity.node.CrossReference;
import org.alliancegenome.neo4j.entity.node.ExpressionBioEntity;
import org.alliancegenome.neo4j.entity.node.GOTerm;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.MMOTerm;
import org.alliancegenome.neo4j.entity.node.Publication;
import org.alliancegenome.neo4j.entity.node.Stage;
import org.alliancegenome.neo4j.entity.node.UBERONTerm;
import org.alliancegenome.neo4j.repository.GeneRepository;

public class ExpressionService {

    public static final String CELLULAR_COMPONENT = "Subcellular";

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
                        detail.setPublications(bioJoins.stream().map(BioEntityGeneExpressionJoin::getPublication).collect(Collectors.toCollection(TreeSet::new)));
                        detail.setCrossReferences(bioJoins.stream()
                                .map(BioEntityGeneExpressionJoin::getCrossReference)
                                .filter(Objects::nonNull)
                                .collect(toList())
                        );
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
        sortingMapping.put(FieldFilter.STAGE, Comparator.comparing(o -> o.getStage().getPrimaryKey().toUpperCase()));
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
                    .collect(toList());
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

    public ExpressionSummary getExpressionSummary(String id) {
        GeneRepository geneRepository = new GeneRepository();
        List<BioEntityGeneExpressionJoin> joins = geneRepository.getExpressionAnnotationSummary(id);

        if (joins == null)
            joins = new ArrayList<>();
        // group together records where only publication is different and treat them as a single record
        Map<Gene, Map<ExpressionBioEntity, Map<Optional<Stage>, Map<MMOTerm, Set<BioEntityGeneExpressionJoin>>>>> groupedRecords = getGeneTermStageAssayMap(joins);

        ExpressionSummary summary = new ExpressionSummary();
        GeneRepository repository = new GeneRepository();

        // create GO & AO histograms
        // list of all terms over grouped list
        List<UBERONTerm> aoGroupedList = new ArrayList<>();
        List<GOTerm> goGroupedList = new ArrayList<>();
        List<UBERONTerm> stageGroupedList = new ArrayList<>();

        groupedRecords.forEach((gene, termNameMap) -> {
            termNameMap.forEach((entity, stageMap) -> {
                stageMap.forEach((stage, assayMap) -> {
                    assayMap.forEach((assay, bioJoins) -> {
                        // do not loop over bioJoins as they are the pubs per the full grouping.
                        aoGroupedList.addAll(entity.getAoTermList());
                        goGroupedList.addAll(entity.getCcRibbonTermList());
                        // use the first join element (they all have the same stage info
                        UBERONTerm stageTerm = bioJoins.iterator().next().getStageTerm();
                        if (stageTerm != null)
                            stageGroupedList.add(stageTerm);
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

        Map<String, Long> goHistogram = goGroupedList.stream()
                .collect(Collectors.groupingBy(GOTerm::getPrimaryKey, Collectors.counting()));
        ExpressionSummaryGroup goGroup = populateGroupInfo(CELLULAR_COMPONENT, goHistogram, repository.getFullGoList());
        int sumGo = goGroup.getTerms().stream().mapToInt(ExpressionSummaryGroupTerm::getNumberOfAnnotations).sum();
        goGroup.setTotalAnnotations(sumGo);
        summary.addGroup(goGroup);

        Map<String, Long> stageHistogram = stageGroupedList.stream()
                .collect(Collectors.groupingBy(UBERONTerm::getPrimaryKey, Collectors.counting()));

        ExpressionSummaryGroup stageGroup = populateGroupInfo("Stage", stageHistogram, repository.getStageList());
        summary.addGroup(stageGroup);

        int sumStage = stageGroup.getTerms().stream().mapToInt(ExpressionSummaryGroupTerm::getNumberOfAnnotations).sum();
        stageGroup.setTotalAnnotations(sumStage);

        // add all annotations per goCcGroup
        summary.setTotalAnnotations(sumGo + sumAO + sumStage);
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
        headerJoiner.add("Gene ID");
        headerJoiner.add("Term");
        headerJoiner.add("Stage");
        headerJoiner.add("Assay");
        headerJoiner.add("Source");
        headerJoiner.add("Reference");
        builder.append(headerJoiner.toString());
        builder.append(ConfigHelper.getJavaLineSeparator());
        result.getResults().forEach(expressionDetail -> {
            StringJoiner rowJoiner = new StringJoiner("\t");
            rowJoiner.add(expressionDetail.getGene().getSpecies().getName());
            rowJoiner.add(expressionDetail.getGene().getSymbol());
            rowJoiner.add(expressionDetail.getGene().getModGlobalId());
            rowJoiner.add(expressionDetail.getTermName());
            rowJoiner.add(expressionDetail.getStage().getPrimaryKey());
            rowJoiner.add(expressionDetail.getAssay().getDisplay_synonym());
            rowJoiner.add(expressionDetail.getCrossReferences().stream().map(CrossReference::getDisplayName).collect(Collectors.joining(",")));
            rowJoiner.add(expressionDetail.getPublications().stream().map(Publication::getPubId).collect(Collectors.joining(",")));
            builder.append(rowJoiner.toString());
            builder.append(ConfigHelper.getJavaLineSeparator());
        });
        return builder.toString();
    }
}
