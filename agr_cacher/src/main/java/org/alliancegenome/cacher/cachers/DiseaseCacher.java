package org.alliancegenome.cacher.cachers;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.api.service.DiseaseRibbonService;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.manager.BasicCachingManager;
import org.alliancegenome.cache.repository.DiseaseCacheRepository;
import org.alliancegenome.core.service.DiseaseAnnotationSorting;
import org.alliancegenome.core.service.SortingField;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@Log4j2
public class DiseaseCacher extends Cacher {

    private static DiseaseRepository diseaseRepository = new DiseaseRepository();
    private static DiseaseCacheRepository diseaseCacheRepository = new DiseaseCacheRepository();
    private BasicCachingManager manager = new BasicCachingManager();

    protected void cache() {

        // model type of diseases
        populateModelsWithDiseases();

        startProcess("diseaseRepository.getAllDiseaseEntityJoins");
        Set<DiseaseEntityJoin> joinList = diseaseRepository.getAllDiseaseEntityJoins();
        if (joinList == null)
            return;

        if (useCache) {
            joinList = joinList.stream()
                    .filter(diseaseEntityJoin -> diseaseEntityJoin.getGene() != null)
                    //.filter(diseaseEntityJoin -> diseaseEntityJoin.getAllele() != null)
                    .filter(diseaseEntityJoin -> diseaseEntityJoin.getGene().getPrimaryKey().equals("HGNC:7"))
                    //.filter(diseaseEntityJoin -> diseaseEntityJoin.getAllele().getPrimaryKey().equals("FB:FBgn0030343"))
                    //.filter(diseaseEntityJoin -> diseaseEntityJoin.getGene().getPrimaryKey().equals("FB:FBgn0030343"))
                    .collect(toSet());
        }
        finishProcess();

        startProcess("Add PAEs to DiseaseAnnotations");
        List<DiseaseAnnotation> allDiseaseAnnotations = getDiseaseAnnotationsFromDEJs(joinList);
        finishProcess();

        log.info("Number of DiseaseAnnotation object before merge: " + String.format("%,d", allDiseaseAnnotations.size()));
        // merge disease Annotations with the same
        // disease / gene / association type combination
        mergeDiseaseAnnotationsByAGM(allDiseaseAnnotations);
        log.info("Number of DiseaseAnnotation object after merge: " + String.format("%,d", allDiseaseAnnotations.size()));


        // default sorting
        DiseaseAnnotationSorting sorting = new DiseaseAnnotationSorting();
        allDiseaseAnnotations.sort(sorting.getComparator(SortingField.DEFAULT, Boolean.TRUE));

        Map<String, Set<String>> closureMapping = diseaseRepository.getClosureMapping();
        log.info("Number of Disease IDs: " + closureMapping.size());
        final Set<String> allIDs = closureMapping.keySet();

        // loop over all disease IDs (termID)
        // and store the annotations in a map for quick retrieval

        Map<String, List<DiseaseAnnotation>> diseaseAnnotationTermMap = allDiseaseAnnotations.stream()
                .collect(groupingBy(annotation -> annotation.getDisease().getPrimaryKey()));

        Map<String, List<DiseaseAnnotation>> diseaseAnnotationMap = new HashMap<>();
        allIDs.forEach(termID -> {
            Set<String> allDiseaseIDs = closureMapping.get(termID);
            List<DiseaseAnnotation> allAnnotations = new ArrayList<>();
            allDiseaseIDs.stream()
                    .filter(id -> diseaseAnnotationTermMap.get(id) != null)
                    .forEach(id -> allAnnotations.addAll(diseaseAnnotationTermMap.get(id)));
            diseaseAnnotationMap.put(termID, allAnnotations);
        });

        // Create map with genes as keys and their associated disease annotations as values
        // Map<gene ID, List<DiseaseAnnotation>> including annotations to child terms
        Map<String, List<DiseaseAnnotation>> diseaseAnnotationExperimentGeneMap = allDiseaseAnnotations.stream()
                .filter(annotation -> annotation.getSortOrder() < 10)
                .filter(annotation -> annotation.getGene() != null)
                .collect(groupingBy(o -> o.getGene().getPrimaryKey(), Collectors.toList()));
        diseaseAnnotationMap.putAll(diseaseAnnotationExperimentGeneMap);

        log.info("Number of Disease IDs in disease Map after adding gene grouping: " + diseaseAnnotationMap.size());

        storeIntoCache(allDiseaseAnnotations, diseaseAnnotationMap, CacheAlliance.DISEASE_ANNOTATION);

        // take care of allele
        if (populateAllelesCache(closureMapping, allIDs)) return;


        diseaseRepository.clearCache();

    }

    private void storeIntoCache(List<DiseaseAnnotation> diseaseAnnotations, Map<String, List<DiseaseAnnotation>> diseaseAnnotationMap, CacheAlliance cacheSpace) {
        startProcess(cacheSpace.name() + " into cache", diseaseAnnotationMap.size());
        diseaseAnnotationMap.forEach((key, value) -> {
            manager.setCache(key, value, View.DiseaseCacher.class, cacheSpace);
            progressProcess();
        });
        CacheStatus status = new CacheStatus(cacheSpace);
        status.setNumberOfEntities(diseaseAnnotations.size());

        Map<String, List<DiseaseAnnotation>> speciesStats = diseaseAnnotations.stream()
                .filter(annotation -> annotation.getGene() != null)
                .collect(groupingBy(annotation -> annotation.getGene().getSpecies().getName()));

        Map<String, Integer> stats = new TreeMap<>();
        diseaseAnnotationMap.forEach((diseaseID, annotations) -> stats.put(diseaseID, annotations.size()));

        Arrays.stream(SpeciesType.values())
                .filter(speciesType -> !speciesStats.keySet().contains(speciesType.getName()))
                .forEach(speciesType -> speciesStats.put(speciesType.getName(), new ArrayList<>()));

        Map<String, Integer> speciesStatsInt = new HashMap<>();
        speciesStats.forEach((species, alleles) -> speciesStatsInt.put(species, alleles.size()));

        status.setEntityStats(stats);
        status.setSpeciesStats(speciesStatsInt);
        setCacheStatus(status);
        finishProcess();
    }

    private void populateModelsWithDiseases() {
        // model type of diseases
        List<DiseaseEntityJoin> pureAgmDiseases = diseaseRepository.getAllDiseaseAnnotationsPureAGM();
        log.info("Retrieved " + String.format("%,d", pureAgmDiseases.size()) + " DiseaseEntityJoin records for pure AGMs");
        // set the gene object on the join


        // phenotypeEntityJoin PK, List<Gene>
        Map<String, List<Gene>> modelGenesMap = new HashMap<>();

        pureAgmDiseases.stream()
                .filter(join -> org.apache.commons.collections.CollectionUtils.isNotEmpty(join.getModel().getAlleles()))
                .forEach(join -> {
                    Set<Gene> geneList = join.getModel().getAlleles().stream()
                            .map(Allele::getGene)
                            .collect(toSet());
                    final String primaryKey = join.getPrimaryKey();
                    List<Gene> genes = modelGenesMap.get(primaryKey);
                    if (genes == null) {
                        genes = new ArrayList<>();
                    }
                    genes.addAll(geneList);
                    genes = genes.stream().distinct().collect(toList());
                    modelGenesMap.put(primaryKey, genes);
                });
        pureAgmDiseases.stream()
                .filter(join -> org.apache.commons.collections.CollectionUtils.isNotEmpty(join.getModel().getAlleles()))
                .forEach(join -> {
                    Set<Gene> geneList = join.getModel().getSequenceTargetingReagents().stream()
                            .map(SequenceTargetingReagent::getGene)
                            .collect(toSet());
                    final String primaryKey = join.getPrimaryKey();
                    List<Gene> genes = modelGenesMap.get(primaryKey);
                    if (genes == null) {
                        genes = new ArrayList<>();
                    }
                    genes.addAll(geneList);
                    genes = genes.stream().distinct().collect(toList());
                    modelGenesMap.put(primaryKey, genes);
                });

        List<DiseaseAnnotation> allDiseaseAnnotationsPure = pureAgmDiseases.stream()
                .map(join -> {
                    DiseaseAnnotation document = new DiseaseAnnotation();
                    final AffectedGenomicModel model = join.getModel();
                    document.setModel(model);
                    document.setPrimaryKey(join.getPrimaryKey());
                    document.setDisease(join.getDisease());
                    document.setPublications(join.getPublications());

                    PrimaryAnnotatedEntity entity = new PrimaryAnnotatedEntity();
                    entity.setId(model.getPrimaryKey());
                    entity.setEntityJoinPk(join.getPrimaryKey());
                    entity.setName(model.getName());
                    entity.setDisplayName(model.getNameText());
                    entity.setUrl(model.getModCrossRefCompleteUrl());
                    entity.setType(GeneticEntity.CrossReferenceType.getCrossReferenceType(model.getSubtype()));
                    entity.addPublicationEvidenceCode(join.getPublicationJoins());
                    entity.addDisease(join.getDisease());
                    entity.setDataProvider(model.getDataProvider());
                    document.addPrimaryAnnotatedEntity(entity);
                    return document;
                })
                .collect(Collectors.toList());

        Map<String, DiseaseAnnotation> paMap = allDiseaseAnnotationsPure.stream()
                .collect(toMap(DiseaseAnnotation::getPrimaryKey, entity -> entity));
        // merge annotations with the same model
        // geneID, Map<modelID, List<PhenotypeAnnotation>>>
/*
        Map<String, Map<String, List<PhenotypeAnnotation>>> annotationPureMergeMap = allDiseaseAnnotationsPure.stream()
                .collect(groupingBy(phenotypeAnnotation -> phenotypeAnnotation.getGene().getPrimaryKey(), groupingBy(annotation -> annotation.getModel().getPrimaryKey())));
*/
        Map<String, Map<String, List<DiseaseAnnotation>>> annotationPureMergeMap = new HashMap<>();

        modelGenesMap.forEach((diseaseEntityJoinID, genes) -> {
            DiseaseAnnotation diseaseAnnot = paMap.get(diseaseEntityJoinID);

            genes.forEach(gene -> {
                Map<String, List<DiseaseAnnotation>> annotations = annotationPureMergeMap.get(gene.getPrimaryKey());
                if (annotations == null) {
                    annotations = new HashMap<>();
                    annotationPureMergeMap.put(gene.getPrimaryKey(), annotations);
                }

                List<DiseaseAnnotation> dease = annotations.get(diseaseAnnot.getModel().getPrimaryKey());
                if (dease == null) {
                    dease = new ArrayList<>();
                    annotations.put(diseaseAnnot.getModel().getPrimaryKey(), dease);
                }
                dease.add(diseaseAnnot);
            });
        });


        Map<String, List<PrimaryAnnotatedEntity>> diseaseAnnotationPureMap = new HashMap<>();
        annotationPureMergeMap.forEach((geneID, modelIdMap) -> modelIdMap.forEach((modelID, diseaseAnnotations) -> {
            List<PrimaryAnnotatedEntity> mergedAnnotations = diseaseAnnotationPureMap.get(geneID);
            if (mergedAnnotations == null)
                mergedAnnotations = new ArrayList<>();
            PrimaryAnnotatedEntity entity = diseaseAnnotations.get(0).getPrimaryAnnotatedEntities().get(0);
            diseaseAnnotations.forEach(diseaseAnnotation -> {
                entity.addDisease(diseaseAnnotation.getDisease());
                entity.addPublicationEvidenceCode(diseaseAnnotation.getPrimaryAnnotatedEntities().get(0).getPublicationEvidenceCodes());
            });
            mergedAnnotations.add(entity);
            diseaseAnnotationPureMap.put(geneID, mergedAnnotations);
        }));

        BasicCachingManager managerModel = new BasicCachingManager();

        diseaseAnnotationPureMap.forEach((geneID, value) -> {
            if (geneID.equals("MGI:104798")) {
                log.info("found gene: " + geneID + " with annotations: " + value.size());
                //result.getResults().forEach(entity -> log.info(entity.getId()));
            }
            managerModel.setCache(geneID, value, View.PrimaryAnnotation.class, CacheAlliance.GENE_PURE_AGM_DISEASE);
            progressProcess();
        });
    }

    private boolean populateAllelesCache(Map<String, Set<String>> closureMapping, Set<String> allIDs) {

        Set<DiseaseEntityJoin> alleleEntityJoins = diseaseRepository.getAllDiseaseAlleleEntityJoins();
        List<DiseaseAnnotation> alleleList = getDiseaseAnnotationsFromDEJs(alleleEntityJoins);
        if (alleleList == null)
            return true;

        log.info("Number of DiseaseAnnotation objects with Alleles: " + String.format("%,d", alleleList.size()));
        Map<String, List<DiseaseAnnotation>> diseaseAlleleAnnotationTermMap = alleleList.stream()
                .collect(groupingBy(annotation -> annotation.getDisease().getPrimaryKey()));

        Map<String, List<DiseaseAnnotation>> diseaseAlleleAnnotationMap = new HashMap<>();
        allIDs.forEach(termID -> {
            Set<String> allDiseaseIDs = closureMapping.get(termID);
            List<DiseaseAnnotation> allAnnotations = new ArrayList<>();
            allDiseaseIDs.stream()
                    .filter(id -> diseaseAlleleAnnotationTermMap.get(id) != null)
                    .forEach(id -> allAnnotations.addAll(diseaseAlleleAnnotationTermMap.get(id)));
            diseaseAlleleAnnotationMap.put(termID, allAnnotations);
        });

        storeIntoCache(alleleList, diseaseAlleleAnnotationMap, CacheAlliance.DISEASE_ALLELE_ANNOTATION);

        // <AlleleID, List of DAs>
        Map<String, List<DiseaseAnnotation>> diseaseAlleleMap = alleleList.stream()
                .collect(groupingBy(annotation -> annotation.getFeature().getPrimaryKey()));
        storeIntoCache(alleleList, diseaseAlleleMap, CacheAlliance.ALLELE_DISEASE);

        return false;
    }

    private List<DiseaseAnnotation> getDiseaseAnnotationsFromDEJs(Collection<DiseaseEntityJoin> joinList) {
        DiseaseRibbonService diseaseRibbonService = new DiseaseRibbonService();

        return joinList.stream()
                .map(join -> {
                    DiseaseAnnotation document = new DiseaseAnnotation();
                    document.setPrimaryKey(join.getPrimaryKey());
                    document.setGene(join.getGene());
                    document.setFeature(join.getAllele());
                    document.setModel(join.getModel());
                    document.setDisease(join.getDisease());
                    document.setSource(join.getSource());
                    document.setAssociationType(join.getJoinType());
                    document.setSortOrder(join.getSortOrder());
                    if (join.getSourceProvider() != null) {
                        Map<String, CrossReference> providerMap = new HashMap<>();
                        providerMap.put("sourceProvider", join.getSourceProvider());
                        if (join.getLoadProvider() != null)
                            providerMap.put("loadProvider", join.getLoadProvider());
                        document.setProviders(providerMap);
                    }
                    List<Gene> orthologyGenes = join.getOrthologyGenes();
                    if (orthologyGenes != null) {
                        orthologyGenes.sort(Comparator.comparing(gene -> gene.getSymbol().toLowerCase()));
                        document.setOrthologyGenes(orthologyGenes);
                    }

                    // used to populate the DOTerm object on the PrimaryAnnotationEntity object
                    // Needed as the same AGM can be reused on multiple pubJoin nodes.
                    Map<String, PrimaryAnnotatedEntity> entities = new HashMap<>();

                    // sort to ensure subsequent caching processes will generate the same PAEs with the
                    // same PK. Note the merging that is happening
                    List<PublicationJoin> publicationJoins1 = join.getPublicationJoins();
                    publicationJoins1.sort(Comparator.comparing(PublicationJoin::getPrimaryKey));
                    if (CollectionUtils.isNotEmpty(publicationJoins1)) {
                        // create PAEs from AGMs
                        join.getPublicationJoins()
                                .stream()
                                .filter(pubJoin -> pubJoin.getModel() != null)
                                .forEach(pubJoin -> {
                                    AffectedGenomicModel model = pubJoin.getModel();
                                    PrimaryAnnotatedEntity entity = entities.get(model.getPrimaryKey());
                                    if (entity == null) {
                                        entity = new PrimaryAnnotatedEntity();
                                        entity.setId(model.getPrimaryKey());
                                        entity.setName(model.getName());
                                        entity.setUrl(model.getModCrossRefCompleteUrl());
                                        entity.setDisplayName(model.getNameText());
                                        entity.setType(GeneticEntity.getType(model.getSubtype()));
                                        entities.put(model.getPrimaryKey(), entity);
                                    }
                                    document.addPrimaryAnnotatedEntity(entity);
                                    entity.addPublicationEvidenceCode(pubJoin);
                                    entity.addDisease(join.getDisease());
                                });
                        // create PAEs from Alleles
                        join.getPublicationJoins()
                                .stream()
                                .filter(pubJoin -> CollectionUtils.isNotEmpty(pubJoin.getAlleles()))
                                .forEach(pubJoin -> pubJoin.getAlleles().forEach(allele -> {
                                    PrimaryAnnotatedEntity entity = entities.get(allele.getPrimaryKey());
                                    if (entity == null) {
                                        entity = new PrimaryAnnotatedEntity();
                                        entity.setId(allele.getPrimaryKey());
                                        entity.setName(allele.getSymbol());
                                        List<CrossReference> refs = allele.getCrossReferences();
                                        if (org.apache.commons.collections.CollectionUtils.isNotEmpty(refs))
                                            entity.setUrl(refs.get(0).getCrossRefCompleteUrl());

                                        entity.setDisplayName(allele.getSymbolText());
                                        entity.setType(GeneticEntity.CrossReferenceType.ALLELE);
                                        entities.put(allele.getPrimaryKey(), entity);
                                    }
                                    document.addPrimaryAnnotatedEntity(entity);
                                    entity.addPublicationEvidenceCode(pubJoin);
                                    entity.addDisease(join.getDisease());
                                }));
                    }
                    List<PublicationJoin> publicationJoins = join.getPublicationJoins();
                    if (useCache) {
                        diseaseCacheRepository.populatePublicationJoinsFromCache(join.getPublicationJoins());
                    } else {
                        diseaseRepository.populatePublicationJoins(publicationJoins);
                    }
                    document.setPublicationJoins(publicationJoins);
    /*
                        List<ECOTerm> ecoList = join.getPublicationJoins().stream()
                                .filter(join -> CollectionUtils.isNotEmpty(join.getEcoCode()))
                                .map(PublicationJoin::getEcoCode)
                                .flatMap(Collection::stream).sorted(Comparator.naturalOrder()).collect(Collectors.toList());
                        document.setEcoCodes(ecoList.stream().distinct().collect(Collectors.toList()));
    */
                    // work around as I cannot figure out how to include the ECOTerm in the overall query without slowing down the performance.
                    List<ECOTerm> evidences;
                    if (useCache) {
                        evidences = diseaseCacheRepository.getEcoTermsFromCache(join.getPublicationJoins());
                    } else {
                        evidences = diseaseRepository.getEcoTerm(join.getPublicationJoins());
                        Set<String> slimId = diseaseRibbonService.getAllParentIDs(join.getDisease().getPrimaryKey());
                        document.setParentIDs(slimId);
                    }
                    document.setEcoCodes(evidences);
                    progressProcess();
                    return document;
                })
                .collect(toList());
    }

    private void mergeDiseaseAnnotationsByAGM(List<DiseaseAnnotation> allDiseaseAnnotations) {
        Map<DOTerm, Map<Gene, Map<String, List<DiseaseAnnotation>>>> multiIndex = allDiseaseAnnotations.stream()
                .filter(diseaseAnnotation -> diseaseAnnotation.getGene() != null)
                .collect(groupingBy(DiseaseAnnotation::getDisease,
                        groupingBy(DiseaseAnnotation::getGene,
                                groupingBy(DiseaseAnnotation::getAssociationType))));

        multiIndex.forEach((doTerm, geneMapMap) -> {
            geneMapMap.forEach((gene, stringListMap) -> {
                stringListMap.forEach((type, diseaseAnnotations) -> {
                    if (type.equals("is_implicated_in") && diseaseAnnotations.size() > 1) {

                        List<PrimaryAnnotatedEntity> models = diseaseAnnotations.stream()
                                .filter(diseaseAnnotation -> diseaseAnnotation.getPrimaryAnnotatedEntities() != null)
                                .map(DiseaseAnnotation::getPrimaryAnnotatedEntities)
                                .flatMap(Collection::stream)
                                .collect(toList());

                        // only merge annotations that have at least one PAE
/*
                        if (CollectionUtils.isEmpty(models))
                            return;
*/
                        // sort so we always pick the same base annotation to merge into
                        diseaseAnnotations.sort(Comparator.comparing(DiseaseAnnotation::getPrimaryKey));
                        DiseaseAnnotation annotation = diseaseAnnotations.get(0);
                        int index = 0;
                        for (DiseaseAnnotation annot : diseaseAnnotations) {
                            if (index++ == 0)
                                continue;
                            annotation.addAllPrimaryAnnotatedEntities(annot.getPrimaryAnnotatedEntities());
                            annotation.addPublicationJoins(annot.getPublicationJoins());
                            annot.setRemove(true);
                        }
                    }
                });
            });
        });

        allDiseaseAnnotations.removeIf(DiseaseAnnotation::isRemove);
    }

}

