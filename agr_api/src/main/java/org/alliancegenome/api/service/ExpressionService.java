package org.alliancegenome.api.service;

import org.alliancegenome.api.entity.*;
import org.alliancegenome.cache.repository.ExpressionCacheRepository;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.cache.repository.helper.PaginationResult;
import org.alliancegenome.core.ExpressionDetail;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.es.model.query.FieldFilter;
import org.alliancegenome.es.model.query.Pagination;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.MultiValuedMap;
import org.apache.commons.collections4.multimap.ArrayListValuedHashMap;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@RequestScoped
public class ExpressionService {

    @Inject
    private ExpressionCacheRepository expressionCacheRepository;

    @Inject
    private ExpressionRibbonService service;

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

//  private Map<Gene, Map<ExpressionBioEntity, Map<Optional<UBERONTerm>, Map<MMOTerm, Set<BioEntityGeneExpressionJoin>>>>> getGeneTermStageRibbonAssayMap(List<BioEntityGeneExpressionJoin> joins) {
//      // grouping by: gene, term name, ribbon stage, assay
//      // to collate publications / sources
//      return joins.stream()
//              .collect(groupingBy(BioEntityGeneExpressionJoin::getGene, groupingBy(BioEntityGeneExpressionJoin::getEntity,
//                      groupingBy(join -> Optional.ofNullable(join.getStageTerm()), groupingBy(BioEntityGeneExpressionJoin::getAssay, toSet())))));
//  }

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
        RibbonSummary ribbonSummary = service.getRibbonSectionInfo();
        geneIDs.forEach(geneID -> ribbonSummary.addRibbonEntity(getExpressionRibbonSummary(geneID)));
        return ribbonSummary;
    }


    private RibbonEntity getExpressionRibbonSummary(String geneID) {
        GeneRepository geneRepository = new GeneRepository();

        List<ExpressionDetail> expressionList = expressionCacheRepository.getExpressionDetails(geneID);

        // create histograms for each of the three ontologies
        List<ExpressionDetail> uberonAnnotations = new ArrayList<>();
        MultiValuedMap<String, ExpressionDetail> aoUberonMap = new ArrayListValuedHashMap<>();

        List<ExpressionDetail> goAnnotations = new ArrayList<>();
        MultiValuedMap<String, ExpressionDetail> goTermMap = new ArrayListValuedHashMap<>();

        List<ExpressionDetail> stageAnnotations = new ArrayList<>();
        MultiValuedMap<String, ExpressionDetail> stageTermMap = new ArrayListValuedHashMap<>();


        expressionList.forEach(detail -> {
            if (CollectionUtils.isNotEmpty(detail.getUberonTermIDs())) {
                uberonAnnotations.add(detail);
                detail.getUberonTermIDs().forEach(uberonTerm -> aoUberonMap.put(uberonTerm, detail));
            }

            if (CollectionUtils.isNotEmpty(detail.getGoTermIDs())) {
                goAnnotations.add(detail);
                detail.getGoTermIDs().forEach(goTermId -> goTermMap.put(goTermId, detail));
            }

            String stageTermID = detail.getStageTermID();
            if (stageTermID != null) {
                stageAnnotations.add(detail);
                stageTermMap.put(stageTermID, detail);
            }
        });


        Gene gene = geneRepository.getShallowGene(geneID);
        RibbonEntity entity = new RibbonEntity();
        entity.setId(geneID);
        entity.setLabel(gene.getSymbol());
        entity.setTaxonID(gene.getTaxonId());
        entity.setTaxonName(gene.getSpecies().getName());

        // add the AO root term
        EntitySubgroupSlim slimRoot = getEntitySubgroupSlim(ExpressionCacheRepository.UBERON_ANATOMY_ROOT, uberonAnnotations, gene.getSpecies());
        entity.addEntitySlim(slimRoot);
        aoUberonMap.keySet().forEach(uberonTermID -> {
            EntitySubgroupSlim slim = getEntitySubgroupSlim(uberonTermID, aoUberonMap.get(uberonTermID), gene.getSpecies());
            entity.addEntitySlim(slim);
        });
        // populate the empty ribbon cell info
        // need to set the 'available' attribute
        // AO fixup
        service.getRibbonSections().getDiseaseRibbonSections().get(0).getSlims().forEach(section -> {
            if (CollectionUtils.isEmpty(aoUberonMap.get(section.getId()))) {
                EntitySubgroupSlim slim = getEntitySubgroupSlim(section.getId(), null, gene.getSpecies());
                entity.addEntitySlim(slim);
            }
        });

        // add the Stage root term
        EntitySubgroupSlim slimRootStage = getEntitySubgroupStageSlim(ExpressionCacheRepository.UBERON_STAGE_ROOT, stageAnnotations, gene.getSpecies());
        entity.addEntitySlim(slimRootStage);
        stageTermMap.keySet().forEach(uberonTermID -> {
            EntitySubgroupSlim slim = getEntitySubgroupStageSlim(uberonTermID, stageTermMap.get(uberonTermID), gene.getSpecies());
            entity.addEntitySlim(slim);
        });

        // add the GO root term
        EntitySubgroupSlim slimRootGO = getEntitySubgroupSlim(ExpressionCacheRepository.GO_CC_ROOT, goAnnotations, gene.getSpecies());
        entity.addEntitySlim(slimRootGO);
        goTermMap.keySet().forEach(goTermID -> {
            EntitySubgroupSlim slim = getEntitySubgroupSlim(goTermID, goTermMap.get(goTermID), gene.getSpecies());
            entity.addEntitySlim(slim);
        });

        entity.setNumberOfClasses(getDistinctClassSize(expressionList));
        entity.setNumberOfAnnotations(expressionList.size());
        return entity;
    }

    private EntitySubgroupSlim getEntitySubgroupSlim(String primaryKey, Collection<ExpressionDetail> aoAnnotations, Species species) {
        EntitySubgroupSlim slim = new EntitySubgroupSlim();
        slim.setId(primaryKey);
        if (aoAnnotations != null) {
            slim.setNumberOfAnnotations(aoAnnotations.size());
            slim.setNumberOfClasses(getDistinctClassSize(aoAnnotations));
        }
        slim.setAvailable(ConfigHelper.getRibbonTermSpeciesApplicability(primaryKey, species.getType().getDisplayName()));
        return slim;
    }

    private int getDistinctClassSize(Collection<ExpressionDetail> aoAnnotations) {
        return aoAnnotations.stream().collect(groupingBy(ExpressionDetail::getTermName)).size();
    }

    private EntitySubgroupSlim getEntitySubgroupStageSlim(String primaryKey, Collection<ExpressionDetail> stageAnnotations, Species species) {
        EntitySubgroupSlim slim = new EntitySubgroupSlim();
        slim.setId(primaryKey);
        if (stageAnnotations != null) {
            slim.setNumberOfAnnotations(stageAnnotations.size());
            slim.setNumberOfClasses(getDistinctStageClassSize(stageAnnotations));
        }
        slim.setAvailable(ConfigHelper.getRibbonTermSpeciesApplicability(primaryKey, species.getType().getDisplayName()));
        return slim;
    }

    private int getDistinctStageClassSize(Collection<ExpressionDetail> stageAnnotations) {
        return stageAnnotations.stream().map(detail -> detail.getStage().getPrimaryKey()).collect(toSet()).size();
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
        JsonResultResponse<ExpressionDetail> response = new JsonResultResponse<>();
        PaginationResult<ExpressionDetail> joins = expressionCacheRepository.getExpressionAnnotations(geneIDs, termID, pagination);
        response.setResults(joins.getResult());
        response.setTotal(joins.getTotalNumber());
        response.addDistinctFieldValueSupplementalData(joins.getDistinctFieldValueMap());
        return response;
    }

}
