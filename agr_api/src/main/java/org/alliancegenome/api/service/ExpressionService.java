package org.alliancegenome.api.service;

import org.alliancegenome.api.entity.*;
import org.alliancegenome.cache.repository.ExpressionCacheRepository;
import org.alliancegenome.core.ExpressionDetail;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.core.service.PaginationResult;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

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
                        //detail.setPublications(bioJoins.stream().map(BioEntityGeneExpressionJoin::getPublications).collect(Collectors.toCollection(TreeSet::new)));
/*
                        detail.setCrossReferences(bioJoins.stream()
                                .map(BioEntityGeneExpressionJoin::getCrossReference)
                                .filter(Objects::nonNull)
                                .collect(toList())
                        );
*/
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
        // group together records where only publications is different and treat them as a single record
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

        ExpressionSummaryGroup aoGroup = populateGroupInfo("Anatomy", aoHistogram, null, repository.getOrderAoTermList());
        summary.addGroup(aoGroup);

        int sumAO = aoGroup.getTerms().stream().mapToInt(ExpressionSummaryGroupTerm::getNumberOfAnnotations).sum();
        aoGroup.setTotalAnnotations(sumAO);

        Map<String, Long> goHistogram = goGroupedList.stream()
                .collect(Collectors.groupingBy(GOTerm::getPrimaryKey, Collectors.counting()));
        ExpressionSummaryGroup goGroup = populateGroupInfo(CELLULAR_COMPONENT, goHistogram, null, repository.getOrderGoTermList());
        int sumGo = goGroup.getTerms().stream().mapToInt(ExpressionSummaryGroupTerm::getNumberOfAnnotations).sum();
        goGroup.setTotalAnnotations(sumGo);
        summary.addGroup(goGroup);

        Map<String, Long> stageHistogram = stageGroupedList.stream()
                .collect(Collectors.groupingBy(UBERONTerm::getPrimaryKey, Collectors.counting()));

        ExpressionSummaryGroup stageGroup = populateGroupInfo("Stage", stageHistogram, null, repository.getStageList());
        summary.addGroup(stageGroup);

        int sumStage = stageGroup.getTerms().stream().mapToInt(ExpressionSummaryGroupTerm::getNumberOfAnnotations).sum();
        stageGroup.setTotalAnnotations(sumStage);

        // add all annotations per goCcGroup
        summary.setTotalAnnotations(sumGo + sumAO + sumStage);
        return summary;
    }

    public RibbonSummary getExpressionRibbonSummary(List<String> geneIDs) {
        if (geneIDs == null)
            return null;
        ExpressionRibbonService service = new ExpressionRibbonService();
        RibbonSummary ribbonSummary = service.getRibbonSectionInfo();
        geneIDs.forEach(geneID -> ribbonSummary.addRibbonEntity(getExpressionRibbonSummary(geneID)));
        return ribbonSummary;
    }


    private RibbonEntity getExpressionRibbonSummary(String geneID) {
        GeneRepository geneRepository = new GeneRepository();
        List<BioEntityGeneExpressionJoin> joins = geneRepository.getExpressionAnnotationSummary(geneID);

        if (joins == null)
            joins = new ArrayList<>();

        // create histograms for each of the three ontologies
        List<BioEntityGeneExpressionJoin> aoAnnotations = new ArrayList<>();
        MultiValuedMap<UBERONTerm, BioEntityGeneExpressionJoin> aoUberonMap = new ArrayListValuedHashMap<>();

        List<BioEntityGeneExpressionJoin> goAnnotations = new ArrayList<>();
        MultiValuedMap<GOTerm, BioEntityGeneExpressionJoin> goTermMap = new ArrayListValuedHashMap<>();

        List<BioEntityGeneExpressionJoin> stageAnnotations = new ArrayList<>();
        MultiValuedMap<UBERONTerm, BioEntityGeneExpressionJoin> stageUberonMap = new ArrayListValuedHashMap<>();

        joins.forEach(join -> {
            final ExpressionBioEntity entity = join.getEntity();

            if (CollectionUtils.isNotEmpty(entity.getAoTermList())) {
                aoAnnotations.add(join);
                entity.getAoTermList().forEach(uberonTerm -> aoUberonMap.put(uberonTerm, join));
            }

            if (CollectionUtils.isNotEmpty(entity.getCcRibbonTermList())) {
                goAnnotations.add(join);
                entity.getCcRibbonTermList().forEach(goTerm -> goTermMap.put(goTerm, join));
            }
            UBERONTerm stageTerm = join.getStageTerm();
            if (stageTerm != null) {
                stageAnnotations.add(join);
                stageUberonMap.put(stageTerm, join);
            }
        });

        Gene gene = geneRepository.getShallowGene(geneID);
        RibbonEntity entity = new RibbonEntity();
        entity.setId(geneID);
        entity.setLabel(gene.getSymbol());
        entity.setTaxonID(gene.getTaxonId());
        entity.setTaxonName(gene.getSpecies().getName());

        // add the AO root term
        EntitySubgroupSlim slimRoot = getEntitySubgroupSlim(ExpressionCacheRepository.UBERON_ANATOMY_ROOT, aoAnnotations);
        entity.addEntitySlim(slimRoot);
        aoUberonMap.keySet().forEach(uberonTerm -> {
            EntitySubgroupSlim slim = getEntitySubgroupSlim(uberonTerm.getPrimaryKey(), aoUberonMap.get(uberonTerm));
            entity.addEntitySlim(slim);
        });

        // add the Stage root term
        EntitySubgroupSlim slimRootStage = getEntitySubgroupSlim(ExpressionCacheRepository.UBERON_STAGE_ROOT, stageAnnotations);
        entity.addEntitySlim(slimRootStage);
        stageUberonMap.keySet().forEach(uberonTerm -> {
            EntitySubgroupSlim slim = getEntitySubgroupSlim(uberonTerm.getPrimaryKey(), stageUberonMap.get(uberonTerm));
            entity.addEntitySlim(slim);
        });

        // add the GO root term
        EntitySubgroupSlim slimRootGO = getEntitySubgroupSlim(ExpressionCacheRepository.GO_CC_ROOT, goAnnotations);
        entity.addEntitySlim(slimRootGO);
        goTermMap.keySet().forEach(goTerm -> {
            EntitySubgroupSlim slim = getEntitySubgroupSlim(goTerm.getPrimaryKey(), goTermMap.get(goTerm));
            entity.addEntitySlim(slim);
        });

        entity.setNumberOfClasses(getDistinctClassSize(joins));
        entity.setNumberOfAnnotations(joins.size());
        return entity;
    }

    private EntitySubgroupSlim getEntitySubgroupSlim(String primaryKey, Collection<BioEntityGeneExpressionJoin> aoAnnotations) {
        EntitySubgroupSlim slim = new EntitySubgroupSlim();
        slim.setId(primaryKey);
        slim.setNumberOfAnnotations(aoAnnotations.size());
        slim.setNumberOfClasses(getDistinctClassSize(aoAnnotations));
        return slim;
    }

    private int getDistinctClassSize(Collection<BioEntityGeneExpressionJoin> aoAnnotations) {
        return aoAnnotations.stream().collect(groupingBy(join -> join.getEntity().getWhereExpressedStatement())).size();
    }

    private ExpressionSummaryGroup populateGroupInfo(String groupName,
                                                     Map<String, Long> histogram,
                                                     Map<String, Long> rawHistogram,
                                                     Map<String, String> entityList) {
        ExpressionSummaryGroup group = new ExpressionSummaryGroup();
        group.setName(groupName);
        entityList.forEach((id, name) ->
        {
            ExpressionSummaryGroupTerm term = new ExpressionSummaryGroupTerm();
            term.setId(id);
            term.setName(name);
            if (histogram.get(id) != null) {
                term.setNumberOfAnnotations((int) (long) histogram.get(id));
                if (rawHistogram != null && rawHistogram.get(id) != null)
                    term.setNumberOfClasses((int) (long) rawHistogram.get(id));
            }
            group.addGroupTerm(term);
        });
        return group;
    }

    public JsonResultResponse<ExpressionDetail> getExpressionDetails(List<String> geneIDs, String termID, Pagination pagination) {
        JsonResultResponse<ExpressionDetail> ret = new JsonResultResponse<>();
        ExpressionCacheRepository expressionCacheRepository = new ExpressionCacheRepository();
        PaginationResult<ExpressionDetail> joins = expressionCacheRepository.getExpressionAnnotations(geneIDs, termID, pagination);
        ret.setResults(joins.getResult());
        ret.setTotal(joins.getTotalNumber());
        return ret;
    }

}
