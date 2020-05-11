package org.alliancegenome.cacher.cachers;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.api.service.DiseaseRibbonService;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.repository.helper.DiseaseAnnotationSorting;
import org.alliancegenome.cache.repository.helper.SortingField;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.*;

@Log4j2
public class DiseaseCacher extends Cacher {

    private static DiseaseRepository diseaseRepository = new DiseaseRepository();

    protected void cache() {

        // model type of diseases
        populateModelsWithDiseases();

        startProcess("diseaseRepository.getAllDiseaseEntityGeneJoins");
        Set<DiseaseEntityJoin> joinList = diseaseRepository.getAllDiseaseEntityGeneJoins();
        if (joinList == null)
            return;

        if (useCache) {
            joinList = joinList.stream()
                    .filter(diseaseEntityJoin -> diseaseEntityJoin.getGene() != null)
                    .filter(diseaseEntityJoin -> diseaseEntityJoin.getGene().getPrimaryKey().equals("SGD:S000005844") ||
                            diseaseEntityJoin.getGene().getPrimaryKey().equals("MGI:109583"))
                    //.filter(diseaseEntityJoin -> diseaseEntityJoin.getGene().getPrimaryKey().equals("FB:FBgn0030343"))
                    .collect(toSet());
        }
        finishProcess();

        startProcess("Add PAEs to DiseaseAnnotations");
        List<DiseaseAnnotation> allDiseaseAnnotations = getDiseaseAnnotationsFromDEJs(joinList);
        finishProcess();

        joinList.clear();


        log.info("Number of DiseaseAnnotation object before merge: " + String.format("%,d", allDiseaseAnnotations.size()));
        // merge disease Annotations with the same
        // disease / gene / association type combination
        //mergeDiseaseAnnotationsByAGM(allDiseaseAnnotations);
        log.info("Number of DiseaseAnnotation object after merge: " + String.format("%,d", allDiseaseAnnotations.size()));


        // default sorting
        DiseaseAnnotationSorting sorting = new DiseaseAnnotationSorting();
        allDiseaseAnnotations.sort(sorting.getComparator(SortingField.DEFAULT, Boolean.TRUE));


        // loop over all disease IDs (termID)
        // and store the annotations in a map for quick retrieval

        Map<String, List<DiseaseAnnotation>> diseaseAnnotationTermMap = allDiseaseAnnotations.stream()
                .collect(groupingBy(annotation -> annotation.getDisease().getPrimaryKey()));

        // add annotations for all parent terms
        Map<String, List<DiseaseAnnotation>> diseaseAnnotationMap = getAnnotationMapIncludingClosure(diseaseAnnotationTermMap);

        log.info("Number of IDs in Map before adding gene IDs: " + diseaseAnnotationMap.size());

        // Create map with genes as keys and their associated disease annotations as values
        // Map<gene ID, List<DiseaseAnnotation>> including annotations to child terms
        Map<String, List<DiseaseAnnotation>> redundantDiseaseAnnotationGeneMap = allDiseaseAnnotations.stream()
                .filter(annotation -> annotation.getSortOrder() < 10)
                .filter(annotation -> annotation.getGene() != null)
                .collect(groupingBy(o -> o.getGene().getPrimaryKey(), Collectors.toList()));
        Map<String, List<DiseaseAnnotation>> diseaseAnnotationExperimentGeneMap = mergeDiseaseAnnotationsForGenes(redundantDiseaseAnnotationGeneMap);

        diseaseAnnotationMap.putAll(diseaseAnnotationExperimentGeneMap);

        redundantDiseaseAnnotationGeneMap.clear();

        log.info("Number of IDs in the Map after adding genes IDs: " + diseaseAnnotationMap.size());

        storeIntoCache(allDiseaseAnnotations, diseaseAnnotationMap, CacheAlliance.DISEASE_ANNOTATION_GENE_LEVEL_GENE_DISEASE);

        diseaseAnnotationMap.clear();
        diseaseAnnotationTermMap.clear();
        diseaseAnnotationExperimentGeneMap.clear();
        allDiseaseAnnotations.clear();


        // take care of allele
        populateAllelesCache();

        diseaseRepository.clearCache();

    }

    private Map<String, List<DiseaseAnnotation>> getAnnotationMapIncludingClosure(Map<String, List<DiseaseAnnotation>> diseaseAnnotationTermMap) {
        Map<String, Set<String>> closureMapping = diseaseRepository.getClosureMapping();
        log.info("Number of Disease IDs: " + closureMapping.size());
        final Set<String> allIDs = closureMapping.keySet();

        Map<String, List<DiseaseAnnotation>> diseaseAnnotationMap = new HashMap<>();
        allIDs.forEach(termID -> {
            Set<String> allDiseaseIDs = closureMapping.get(termID);
            List<DiseaseAnnotation> allAnnotations = new ArrayList<>();
            allDiseaseIDs.stream()
                    .filter(id -> diseaseAnnotationTermMap.get(id) != null)
                    .forEach(id -> allAnnotations.addAll(diseaseAnnotationTermMap.get(id)));
            diseaseAnnotationMap.put(termID, allAnnotations);
        });
        return diseaseAnnotationMap;
    }

    private Map<String, List<DiseaseAnnotation>> mergeDiseaseAnnotationsForGenes(Map<String, List<DiseaseAnnotation>> map) {
        if (map == null)
            return null;

        Map<String, List<DiseaseAnnotation>> returnMap = new HashMap<>();
        map.forEach((geneID, annotations) -> {
            // group by association type and Disease
            Map<String, Map<String, List<DiseaseAnnotation>>> groupedDA = annotations.stream()
                    .collect(groupingBy(DiseaseAnnotation::getAssociationType, groupingBy(annotation -> annotation.getDisease().getPrimaryKey())));
            // merge all DAs in a single group
            List<DiseaseAnnotation> distinctAnnotations = new ArrayList<>();
            groupedDA.forEach((assocType, annotationMap) -> {
                annotationMap.forEach((diseaseID, diseaseAnnotations) -> {
                    // if orthologyGenes present do not merge (SGD-specific rule)
                    diseaseAnnotations.stream()
                            .filter(diseaseAnnotation -> CollectionUtils.isNotEmpty(diseaseAnnotation.getOrthologyGenes()))
                            .forEach(distinctAnnotations::add);

                    // remove those from list of annotations that need to be merged.
                    diseaseAnnotations.removeIf(diseaseAnnotation -> CollectionUtils.isNotEmpty(diseaseAnnotation.getOrthologyGenes()));
                    if (CollectionUtils.isEmpty(diseaseAnnotations))
                        return;
                    // use first element as the merging element
                    // merge: primaryAnnotatedEntity, PublicationJoin
                    DiseaseAnnotation annotation = diseaseAnnotations.get(0);
                    diseaseAnnotations.remove(0);
                    diseaseAnnotations.forEach(singleAnnotation -> {
                        annotation.addAllPrimaryAnnotatedEntities(singleAnnotation.getPrimaryAnnotatedEntities());
                        annotation.addPublicationJoins(singleAnnotation.getPublicationJoins());
                    });
                    distinctAnnotations.add(annotation);
                });
            });
            returnMap.put(geneID, distinctAnnotations);
        });
        return returnMap;
    }

    private void storeIntoCache(List<DiseaseAnnotation> diseaseAnnotations, Map<String, List<DiseaseAnnotation>> diseaseAnnotationMap, CacheAlliance cacheSpace) {
        populateCacheFromMap(diseaseAnnotationMap, View.DiseaseCacher.class, cacheSpace);

        log.debug("Calculate statistics...");
        CacheStatus status = new CacheStatus(cacheSpace);
        status.setNumberOfEntities(diseaseAnnotations.size());
        status.setNumberOfEntityIDs(diseaseAnnotationMap.size());
        status.setCollectionEntity(DiseaseAnnotation.class.getSimpleName());
        status.setJsonViewClass(View.DiseaseAnnotationSummary.class.getSimpleName());

        Map<String, List<DiseaseAnnotation>> speciesStats = diseaseAnnotations.stream()
                .collect(groupingBy(annotation -> annotation.getSpecies().getName()));

        Map<String, Integer> stats = new TreeMap<>();
        diseaseAnnotationMap.forEach((diseaseID, annotations) -> stats.put(diseaseID, annotations.size()));

        Arrays.stream(SpeciesType.values())
                .filter(speciesType -> !speciesStats.keySet().contains(speciesType.getName()))
                .forEach(speciesType -> speciesStats.put(speciesType.getName(), new ArrayList<>()));

        Map<String, Integer> speciesStatsInt = new HashMap<>();
        speciesStats.forEach((species, alleles) -> speciesStatsInt.put(species, alleles.size()));

        Map<String, Integer> sortedMap = speciesStatsInt
                .entrySet()
                .stream()
                .sorted(Collections.reverseOrder(comparingByValue()))
                .collect(toMap(Map.Entry::getKey, Map.Entry::getValue, (e1, e2) -> e2, LinkedHashMap::new));

        status.setEntityStats(stats);
        status.setSpeciesStats(sortedMap);
        setCacheStatus(status);
        log.debug("Finished calculating statistics");
        finishProcess();
    }

    public void storeIntoCachePAE(Map<String, List<PrimaryAnnotatedEntity>> primaryAnnotatedMap, CacheAlliance cacheSpace) {
        populateCacheFromMap(primaryAnnotatedMap, View.PrimaryAnnotation.class, cacheSpace);
        log.debug("Calculate statistics...");
        CacheStatus status = new CacheStatus(cacheSpace);
        status.setNumberOfEntityIDs(primaryAnnotatedMap.size());
        status.setCollectionEntity(PrimaryAnnotatedEntity.class.getSimpleName());
        status.setJsonViewClass(View.PrimaryAnnotation.class.getSimpleName());

        List<PrimaryAnnotatedEntity> annotatedEntities = primaryAnnotatedMap.values().stream()
                .flatMap(Collection::stream)
                .collect(toList());
        status.setNumberOfEntities(annotatedEntities.size());

        Map<String, List<Species>> speciesStats = annotatedEntities.stream()
                .map(PrimaryAnnotatedEntity::getSpecies)
                .collect(groupingBy(Species::getName));

        Map<String, Integer> entityStats = new TreeMap<>();
        primaryAnnotatedMap.forEach((geneID, alleles) -> entityStats.put(geneID, alleles.size()));

        populateStatisticsOnStatus(status, entityStats, speciesStats);
        setCacheStatus(status);

        log.debug("Finished calculating statistics");
        finishProcess();
    }

    private void populateModelsWithDiseases() {
        // disease annotations for models (AGMs)
        List<DiseaseEntityJoin> modelDiseaseJoins = diseaseRepository.getAllDiseaseAnnotationsModelLevel();
        log.info("Retrieved " + String.format("%,d", modelDiseaseJoins.size()) + " DiseaseEntityJoin records for AGMs");

        // diseaseEntityJoin PK, List<Gene>
        Map<String, List<Gene>> modelGenesMap = new HashMap<>();

        modelDiseaseJoins.stream()
                .filter(join -> CollectionUtils.isNotEmpty(join.getModel().getAlleles()))
                .forEach(join -> {
                    Set<Gene> geneList = join.getModel().getAlleles().stream()
                            .map(Allele::getGene)
                            .collect(toSet());
                    geneList.removeIf(Objects::isNull);
                    if (CollectionUtils.isEmpty(geneList))
                        return;
                    final String primaryKey = join.getPrimaryKey();
                    List<Gene> genes = modelGenesMap.get(primaryKey);
                    if (genes == null) {
                        genes = new ArrayList<>();
                    }
                    genes.addAll(geneList);
                    genes = genes.stream().distinct().collect(toList());
                    modelGenesMap.put(primaryKey, genes);
                });
        modelDiseaseJoins.stream()
                .filter(join -> CollectionUtils.isNotEmpty(join.getModel().getSequenceTargetingReagents()))
                .forEach(join -> {
                    Set<Gene> geneList = join.getModel().getSequenceTargetingReagents().stream()
                            .map(SequenceTargetingReagent::getGene)
                            .collect(toSet());
                    geneList.removeIf(Objects::isNull);
                    if (CollectionUtils.isEmpty(geneList))
                        return;
                    final String primaryKey = join.getPrimaryKey();
                    List<Gene> genes = modelGenesMap.get(primaryKey);
                    if (genes == null) {
                        genes = new ArrayList<>();
                    }
                    genes.addAll(geneList);
                    genes = genes.stream().distinct().collect(toList());
                    modelGenesMap.put(primaryKey, genes);
                });

        List<DiseaseAnnotation> diseaseModelAnnotations = modelDiseaseJoins.stream()
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
                    entity.setSpecies(model.getSpecies());
                    document.addPrimaryAnnotatedEntity(entity);
                    document.addPublicationJoins(join.getPublicationJoins());
                    document.setSource(entity.getSource());
                    document.setEcoCodes(join.getEvidenceCodes());
                    return document;
                })
                .collect(Collectors.toList());

        Map<String, List<DiseaseAnnotation>> diseaseModelsMap = diseaseModelAnnotations.stream()
                .collect(groupingBy(annotation -> annotation.getDisease().getPrimaryKey()));

        // add annotations for all parent terms
        Map<String, List<DiseaseAnnotation>> diseaseAnnotationMap = getAnnotationMapIncludingClosure(diseaseModelsMap);


        storeIntoCache(diseaseModelAnnotations, diseaseAnnotationMap, CacheAlliance.DISEASE_ANNOTATION_MODEL_LEVEL_MODEL);
        modelDiseaseJoins.clear();

        Map<String, DiseaseAnnotation> diseaseAnnotationMap1 = diseaseModelAnnotations.stream()
                .collect(toMap(DiseaseAnnotation::getPrimaryKey, entity -> entity));

        diseaseModelAnnotations.clear();

        // merge annotations with the same model
        // geneID, Map<modelID, List<PhenotypeAnnotation>>>
/*
        Map<String, Map<String, List<PhenotypeAnnotation>>> diseaseModelGeneMap = diseaseModelAnnotations.stream()
                .collect(groupingBy(phenotypeAnnotation -> phenotypeAnnotation.getGene().getPrimaryKey(), groupingBy(annotation -> annotation.getModel().getPrimaryKey())));
*/


        // get index by geneID
        // <geneID, Map<modelID, List<DiseaseAnnotation>>
        Map<String, Map<String, List<DiseaseAnnotation>>> diseaseModelGeneMap = new HashMap<>();

        modelGenesMap.forEach((diseaseEntityJoinID, genes) -> {
            DiseaseAnnotation diseaseAnnot = diseaseAnnotationMap1.get(diseaseEntityJoinID);

            genes.forEach(gene -> {
                Map<String, List<DiseaseAnnotation>> annotations = diseaseModelGeneMap.get(gene.getPrimaryKey());
                if (annotations == null) {
                    annotations = new HashMap<>();
                    diseaseModelGeneMap.put(gene.getPrimaryKey(), annotations);
                }

                List<DiseaseAnnotation> dease = annotations.get(diseaseAnnot.getModel().getPrimaryKey());
                if (dease == null) {
                    dease = new ArrayList<>();
                    annotations.put(diseaseAnnot.getModel().getPrimaryKey(), dease);
                }
                dease.add(diseaseAnnot);
            });
        });
        diseaseAnnotationMap.clear();
        diseaseAnnotationMap1.clear();
        modelGenesMap.clear();

        Map<String, List<PrimaryAnnotatedEntity>> diseaseAnnotationPureMap = new HashMap<>();

        diseaseModelGeneMap.forEach((geneID, modelIdMap) -> modelIdMap.forEach((modelID, diseaseAnnotations) -> {
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

        diseaseModelGeneMap.clear();

        storeIntoCachePAE(diseaseAnnotationPureMap, CacheAlliance.DISEASE_ANNOTATION_MODEL_LEVEL_GENE);
        diseaseAnnotationPureMap.clear();
    }

    private boolean populateAllelesCache() {

        Set<DiseaseEntityJoin> alleleEntityJoins = diseaseRepository.getAllDiseaseAlleleEntityJoins();
        List<DiseaseAnnotation> alleleList = getDiseaseAnnotationsFromDEJs(alleleEntityJoins);
        if (alleleList == null)
            return true;

        alleleEntityJoins.clear();

        log.info("Number of DiseaseAnnotation objects with Alleles: " + String.format("%,d", alleleList.size()));
        Map<String, List<DiseaseAnnotation>> diseaseAlleleAnnotationTermMap = alleleList.stream()
                .collect(groupingBy(annotation -> annotation.getDisease().getPrimaryKey()));

        // add annotations to parent terms
        Map<String, List<DiseaseAnnotation>> diseaseAlleleAnnotationMap = getAnnotationMapIncludingClosure(diseaseAlleleAnnotationTermMap);
        storeIntoCache(alleleList, diseaseAlleleAnnotationMap, CacheAlliance.DISEASE_ANNOTATION_ALLELE_LEVEL_ALLELE);

        diseaseAlleleAnnotationMap.clear();

        // <AlleleID, List of DAs>
        Map<String, List<DiseaseAnnotation>> diseaseAlleleMap = alleleList.stream()
                .collect(groupingBy(annotation -> annotation.getFeature().getPrimaryKey()));
        storeIntoCache(alleleList, diseaseAlleleMap, CacheAlliance.ALLELE_DISEASE);

        alleleList.clear();
        diseaseAlleleMap.clear();
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
                    document.setAssociationType(join.getJoinType().toLowerCase());
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
                        populatePublicationJoinsFromCache(join.getPublicationJoins());
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
                        evidences = getEcoTermsFromCache(join.getPublicationJoins());
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

    public List<ECOTerm> getEcoTerm(PublicationJoin join) {
        if (join == null)
            return null;
        return cacheService.getCacheEntries(join.getPrimaryKey(), CacheAlliance.ECO_MAP);
    }

    public List<ECOTerm> getEcoTermsFromCache(List<PublicationJoin> joins) {
        if (joins == null)
            return null;

        return joins.stream()
                .map(join -> getEcoTerm(join))
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    public void populatePublicationJoinsFromCache(List<PublicationJoin> joins) {
        if (joins == null)
            return;

        joins.forEach(publicationJoin -> {
            List<ECOTerm> cacheValue = getEcoTerm(publicationJoin);
            if (cacheValue != null) {
                publicationJoin.setEcoCode(cacheValue);
            }
        });
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

