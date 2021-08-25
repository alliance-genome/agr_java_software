package org.alliancegenome.cacher.cachers;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.ConditionService;
import org.alliancegenome.neo4j.entity.*;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.repository.PhenotypeRepository;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@Log4j2
public class GenePhenotypeCacher extends Cacher {

    private static PhenotypeRepository phenotypeRepository;

    public GenePhenotypeCacher() {
    }

    @Override
    protected void init() {
        phenotypeRepository = new PhenotypeRepository();
    }

    @Override
    protected void cache() {


        startProcess("GenePhenotypeCacher.getAllPhenotypeAnnotations");
        List<PhenotypeEntityJoin> joinList = phenotypeRepository.getAllPhenotypeAnnotations();
        log.info("Number of Gene-related phenotypes: " + String.format("%,d", joinList.size()));
        log.info("Debug mode: " + useCache);
        finishProcess();

        if (useCache) {
            List<PhenotypeEntityJoin> list = joinList.stream()
                    .filter(join -> join.getPublicationJoins().stream().anyMatch(join1 -> join1.getModels() != null))
                    .filter(join -> join.getPublicationJoins().stream().anyMatch(join1 -> join1.getModels().stream().anyMatch(model -> model.getPrimaryKey().equals("ZFIN:ZDB-GENE-990415-8"))))
                    .collect(Collectors.toList());
        }

        List<PhenotypeAnnotation> allPhenotypeAnnotations = getPhenotypeAnnotations(joinList);
        // geneID, Map<phenotype, List<PhenotypeAnnotation>>
        startProcess("allPhenotypeAnnotations.groupingBy getPhenotype", allPhenotypeAnnotations.size());
        Map<String, Map<String, List<PhenotypeAnnotation>>> annotationMergeMap = allPhenotypeAnnotations.stream()
                .collect(groupingBy(phenotypeAnnotation -> phenotypeAnnotation.getGene().getPrimaryKey(), groupingBy(PhenotypeAnnotation::getPhenotype)));
        finishProcess();

        // merge annotations with the same phenotype
        Map<String, List<PhenotypeAnnotation>> phenotypeAnnotationMap = getMergedPhenotypeMap(annotationMergeMap);

        storeIntoCache(joinList, allPhenotypeAnnotations, phenotypeAnnotationMap, CacheAlliance.GENE_PHENOTYPE);

        allPhenotypeAnnotations.clear();
        annotationMergeMap.clear();
        phenotypeAnnotationMap.clear();

        startProcess("GenePhenotypeCacher.getAllelePhenotypeAnnotations");
        List<PhenotypeEntityJoin> joinListAllele = phenotypeRepository.getAllelePhenotypeAnnotations();
        finishProcess();

        // used to populate the DOTerm object on the PrimaryAnnotationEntity object
        List<PhenotypeAnnotation> allelePhenotypeAnnotations = getPhenotypeAnnotations(joinListAllele);

        // alleleID, Map<phenotype, List<PhenotypeAnnotation>>
        startProcess("allelePhenotypeAnnotations.groupingBy getPhenotype", allelePhenotypeAnnotations.size());
        Map<String, Map<String, List<PhenotypeAnnotation>>> annotationAlleleMergeMap = allelePhenotypeAnnotations.stream()
                .collect(groupingBy(phenotypeAnnotation -> phenotypeAnnotation.getAllele().getPrimaryKey(), groupingBy(PhenotypeAnnotation::getPhenotype)));
        finishProcess();

        // merge annotations with the same phenotype
        Map<String, List<PhenotypeAnnotation>> phenotypeAnnotationAlleleMap = getMergedPhenotypeMap(annotationAlleleMergeMap);


        storeIntoCache(joinList, allelePhenotypeAnnotations, phenotypeAnnotationAlleleMap, CacheAlliance.ALLELE_PHENOTYPE);

        phenotypeAnnotationAlleleMap.clear();
        allelePhenotypeAnnotations.clear();
        annotationAlleleMergeMap.clear();

        joinList.clear();


        startProcess("phenotypeRepository.getAllPhenotypeAnnotationsPureAGM");
        List<PhenotypeEntityJoin> pureAgmPhenotypes = phenotypeRepository.getAllPhenotypeAnnotationsPureAGM();
        log.info("Retrieved " + String.format("%,d", pureAgmPhenotypes.size()) + " PhenotypeEntityJoin records for pure AGMs");
        // set the gene object on the join
        finishProcess();

        // phenotypeEntityJoin PK, List<Gene>
        Map<String, List<Gene>> modelGenesMap = new HashMap<>();

        pureAgmPhenotypes.stream()
                .filter(join -> CollectionUtils.isNotEmpty(join.getModel().getAlleles()))
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
        pureAgmPhenotypes.stream()
                .filter(join -> CollectionUtils.isNotEmpty(join.getModel().getSequenceTargetingReagents()))
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

        List<PhenotypeAnnotation> allPhenotypeAnnotationsPure = pureAgmPhenotypes.stream()
                .map(join -> {
                    PhenotypeAnnotation document = new PhenotypeAnnotation();
                    final AffectedGenomicModel model = join.getModel();
                    document.setModel(model);
                    document.setPrimaryKey(join.getPrimaryKey());
                    document.setPhenotype(join.getPhenotype().getPhenotypeStatement());
                    document.setPublications(join.getPublications());
                    document.addConditions(DiseaseAnnotation.ConditionType.HAS_CONDITION, join.getHasConditionList());
                    document.addConditions(DiseaseAnnotation.ConditionType.INDUCES, join.getInducerConditionList());
                    document.addModifier(DiseaseAnnotation.ConditionType.AMELIORATES, join.getAmeliorateConditionList());
                    document.addModifier(DiseaseAnnotation.ConditionType.EXACERBATES, join.getExacerbateConditionList());

                    PrimaryAnnotatedEntity entity = new PrimaryAnnotatedEntity();
                    entity.setId(model.getPrimaryKey());
                    entity.setEntityJoinPk(join.getPrimaryKey());
                    entity.setName(model.getName());
                    entity.setDisplayName(model.getNameText());
                    entity.setUrl(model.getModCrossRefCompleteUrl());
                    entity.setType(GeneticEntity.CrossReferenceType.getCrossReferenceType(model.getSubtype()));
                    entity.addPublicationEvidenceCode(join.getPublicationJoins());
                    entity.addPhenotype(join.getPhenotype().getPhenotypeStatement());
                    entity.setDataProvider(model.getDataProvider());
                    entity.addConditions(ConditionAnnotation.ConditionType.HAS_CONDITION, join.getHasConditionList());
                    entity.addConditions(ConditionAnnotation.ConditionType.INDUCES, join.getInducerConditionList());
                    entity.addModifier(ConditionAnnotation.ConditionType.AMELIORATES, join.getAmeliorateConditionList());
                    entity.addModifier(ConditionAnnotation.ConditionType.EXACERBATES, join.getExacerbateConditionList());

                    document.addPrimaryAnnotatedEntity(entity);
                    return document;
                })
                .collect(Collectors.toList());

        pureAgmPhenotypes.clear();


        Map<String, PhenotypeAnnotation> paMap = allPhenotypeAnnotationsPure.stream()
                .collect(toMap(PhenotypeAnnotation::getPrimaryKey, entity -> entity));

        allPhenotypeAnnotationsPure.clear();

        // merge annotations with the same model
        // geneID, Map<modelID, List<PhenotypeAnnotation>>>
/*
        Map<String, Map<String, List<PhenotypeAnnotation>>> annotationPureMergeMap = allPhenotypeAnnotationsPure.stream()
                .collect(groupingBy(phenotypeAnnotation -> phenotypeAnnotation.getGene().getPrimaryKey(), groupingBy(annotation -> annotation.getModel().getPrimaryKey())));
*/
        Map<String, Map<String, List<PhenotypeAnnotation>>> annotationPureMergeMap = new HashMap<>();

        modelGenesMap.forEach((phenotypeEntityJoinID, genes) -> {
            PhenotypeAnnotation phenoAnnot = paMap.get(phenotypeEntityJoinID);

            genes.forEach(gene -> {
                Map<String, List<PhenotypeAnnotation>> annotations = annotationPureMergeMap.get(gene.getPrimaryKey());
                if (annotations == null) {
                    annotations = new HashMap<>();
                    annotationPureMergeMap.put(gene.getPrimaryKey(), annotations);
                }

                List<PhenotypeAnnotation> phenos = annotations.get(phenoAnnot.getModel().getPrimaryKey());
                if (phenos == null) {
                    phenos = new ArrayList<>();
                    annotations.put(phenoAnnot.getModel().getPrimaryKey(), phenos);
                }
                phenos.add(phenoAnnot);
            });
        });

        modelGenesMap.clear();
        paMap.clear();


        Map<String, List<PrimaryAnnotatedEntity>> phenotypeAnnotationPureMap = new HashMap<>();

        annotationPureMergeMap.forEach((geneID, modelIdMap) -> modelIdMap.forEach((modelID, phenotypeAnnotations) -> {
            List<PrimaryAnnotatedEntity> mergedAnnotations = phenotypeAnnotationPureMap.computeIfAbsent(geneID, s -> new ArrayList<>());
            phenotypeAnnotations.forEach(phenotypeAnnotation -> {
                mergedAnnotations.add(phenotypeAnnotation.getPrimaryAnnotatedEntities().get(0));
            });
            phenotypeAnnotationPureMap.put(geneID, mergedAnnotations);
        }));

        annotationPureMergeMap.clear();


        startProcess("phenotypeAnnotationPureMap", phenotypeAnnotationPureMap.size());
        phenotypeAnnotationPureMap.forEach((geneID, value) -> {
            cacheService.putCacheEntry(geneID, value, View.PrimaryAnnotation.class, CacheAlliance.GENE_PURE_AGM_PHENOTYPE);
            progressProcess();
        });

        phenotypeAnnotationPureMap.clear();

        finishProcess();
        phenotypeRepository.clearCache();
    }

    private void storeIntoCache(List<PhenotypeEntityJoin> joinList, List<PhenotypeAnnotation> allPhenotypeAnnotations, Map<String, List<PhenotypeAnnotation>> phenotypeAnnotationMap, CacheAlliance cacheSpace) {

        startProcess(cacheSpace.name() + " into cache", phenotypeAnnotationMap.size());
        phenotypeAnnotationMap.forEach((key, value) -> {
            cacheService.putCacheEntry(key, value, View.PhenotypeAPI.class, cacheSpace);
            progressProcess();
        });
        CacheStatus status = new CacheStatus(cacheSpace);
        status.setNumberOfEntities(joinList.size());

        Map<String, List<PhenotypeAnnotation>> speciesStats = allPhenotypeAnnotations.stream()
                .filter(annotation -> annotation.getGene() != null)
                .collect(groupingBy(annotation -> annotation.getGene().getSpecies().getName()));

        Map<String, Integer> stats = new TreeMap<>();
        phenotypeAnnotationMap.forEach((diseaseID, annotations) -> stats.put(diseaseID, annotations.size()));

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

    private Map<String, List<PhenotypeAnnotation>> getMergedPhenotypeMap(Map<String, Map<String, List<PhenotypeAnnotation>>> annotationMergeMap) {
        startProcess("getMergedPhenotypeMap annotationMergeMap", annotationMergeMap.size());
        Map<String, List<PhenotypeAnnotation>> phenotypeAnnotationMap = new HashMap<>();
        annotationMergeMap.forEach((geneID, value) -> {
            List<PhenotypeAnnotation> mergedAnnotations = new ArrayList<>();
            value.forEach((phenotype, phenotypeAnnotations) -> {
                // group all PEJs that do not have an experimental condition
                // all others stay indepeden
                // get first element and put all info from other collection elements.
                PhenotypeAnnotation entity = phenotypeAnnotations.get(0);
                phenotypeAnnotations.stream()
                        .filter(phenotypeAnnotation -> CollectionUtils.isNotEmpty(phenotypeAnnotation.getPrimaryAnnotatedEntities()))
                        .forEach(annotation -> {
                            entity.addPrimaryAnnotatedEntities(annotation.getPrimaryAnnotatedEntities());
                            entity.addPublications(annotation.getPublications());
                        });
                mergedAnnotations.add(entity);
            });
            phenotypeAnnotationMap.put(geneID, mergedAnnotations);
        });
        finishProcess();
        return phenotypeAnnotationMap;
    }

    private List<PhenotypeAnnotation> getPhenotypeAnnotations(List<PhenotypeEntityJoin> joinList) {
        return joinList.stream()
                .map(phenotypeEntityJoin -> {
                    PhenotypeAnnotation document = new PhenotypeAnnotation();
                    final Gene gene = phenotypeEntityJoin.getGene();
                    document.setGene(gene);
                    final Allele feature = phenotypeEntityJoin.getAllele();
                    if (feature != null)
                        document.setAllele(feature);
                    document.setPhenotype(phenotypeEntityJoin.getPhenotype().getPhenotypeStatement());
                    document.setPublications(phenotypeEntityJoin.getPublications());
                    document.setSource(phenotypeEntityJoin.getSource());

                    // if AGMs are present
                    if (CollectionUtils.isNotEmpty(phenotypeEntityJoin.getPublicationJoins())) {
                        boolean hasAGMs = phenotypeEntityJoin.getPublicationJoins().stream()
                                .anyMatch(join -> join.getModels() != null);

                        if (hasAGMs) {
                            phenotypeEntityJoin.getPublicationJoins()
                                    .stream()
                                    .filter(pubJoin -> pubJoin.getModels() != null)
                                    .forEach(pubJoin -> pubJoin.getModels().forEach(model -> {
                                        // keep each new PEJ with exp conditions independent PAE
                                        if (model.getPhenotypeEntityJoins() != null) {
                                            model.getPhenotypeEntityJoins().stream()
                                                    .filter(phenotypeEntityJoin1 -> phenotypeEntityJoin1.getPhenotype().equalsPhenotype(phenotypeEntityJoin.getPhenotype()))
                                                    .forEach(phenotypeEntityJoin1 -> {
                                                        PrimaryAnnotatedEntity entity = new PrimaryAnnotatedEntity();
                                                        entity.setId(model.getPrimaryKey());
                                                        entity.setName(model.getName());
                                                        entity.setUrl(model.getModCrossRefCompleteUrl());
                                                        entity.setDisplayName(model.getNameText());
                                                        entity.setType(GeneticEntity.getType(model.getSubtype()));
                                                        entity.addPhenotype(phenotypeEntityJoin.getPhenotype().getPhenotypeStatement());
                                                        addExperimentalConditions(entity, phenotypeEntityJoin1, phenotypeEntityJoin.getPhenotype().getPhenotypeStatement());

                                                        entity.setDataProvider(phenotypeEntityJoin.getDataProvider());
                                                        entity.addPublicationEvidenceCode(pubJoin);
                                                        document.addPrimaryAnnotatedEntity(entity);
                                                    });
                                        }
                                    }));
                        }
                        // create PAEs from Alleles
                        phenotypeEntityJoin.getPublicationJoins()
                                .stream()
                                .filter(pubJoin -> CollectionUtils.isNotEmpty(pubJoin.getAlleles()))
                                .forEach(pubJoin -> pubJoin.getAlleles().forEach(allele -> {
                                    // keep each new PEJ with exp conditions independent PAE
                                    if (allele.getPhenotypeEntityJoins() != null) {
                                        allele.getPhenotypeEntityJoins()
                                                .forEach(phenotypeEntityJoin1 -> {
                                                    PrimaryAnnotatedEntity entity = new PrimaryAnnotatedEntity();
                                                    entity.setId(allele.getPrimaryKey());
                                                    entity.setName(allele.getSymbol());
                                                    entity.addPhenotype(phenotypeEntityJoin.getPhenotype().getPhenotypeStatement());
                                                    addExperimentalConditions(entity, phenotypeEntityJoin1, phenotypeEntityJoin.getPhenotype().getPhenotypeStatement());

                                                    List<CrossReference> refs = allele.getCrossReferences();
                                                    if (org.apache.commons.collections.CollectionUtils.isNotEmpty(refs))
                                                        entity.setUrl(refs.get(0).getCrossRefCompleteUrl());

                                                    entity.setDisplayName(allele.getSymbolText());
                                                    entity.setType(GeneticEntity.CrossReferenceType.ALLELE);
                                                    document.addPrimaryAnnotatedEntity(entity);
                                                    entity.addPublicationEvidenceCode(pubJoin);
                                                });
                                    }
                                }));
                        // create base-level PAE
                        PrimaryAnnotatedEntity baseLevelPAEs = ConditionService.createBaseLevelPAEs(phenotypeEntityJoin);
                        if (baseLevelPAEs != null)
                            document.addPrimaryAnnotatedEntity(baseLevelPAEs);
                    }
                    progressProcess();
                    return document;
                })
                .collect(toList());
    }

    /**
     * Here we check if the PhenotypeEntityJoin objects are pointing to the phenotype statement given by the one on the entity.
     */
    private void addExperimentalConditions(PrimaryAnnotatedEntity entity, PhenotypeEntityJoin entityJoin, String phenotype) {
        if (entityJoin == null)
            return;
        if (entity.getPhenotypes() != null && entityJoin.getPhenotype() != null && phenotype.equals(entityJoin.getPhenotype().getPhenotypeStatement())) {
            entity.addConditions(ConditionAnnotation.ConditionType.HAS_CONDITION, entityJoin.getHasConditionList());
            entity.addConditions(ConditionAnnotation.ConditionType.INDUCES, entityJoin.getInducerConditionList());
            entity.addModifier(ConditionAnnotation.ConditionType.AMELIORATES, entityJoin.getAmeliorateConditionList());
            entity.addModifier(ConditionAnnotation.ConditionType.EXACERBATES, entityJoin.getExacerbateConditionList());
        }
    }

    @Override
    public void close() {
        phenotypeRepository.close();
    }

}
