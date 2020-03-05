package org.alliancegenome.cacher.cachers;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.manager.BasicCachingManager;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.*;
import org.alliancegenome.neo4j.repository.PhenotypeRepository;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.*;

@Log4j2
public class GenePhenotypeCacher extends Cacher {

    private static PhenotypeRepository phenotypeRepository = new PhenotypeRepository();

    public GenePhenotypeCacher() {
        super();
    }

    @Override
    protected void cache() {



        startProcess("GenePhenotypeCacher.getAllPhenotypeAnnotations");
        List<PhenotypeEntityJoin> joinList_new = phenotypeRepository.getAllPhenotypeAnnotations();
        finishProcess();
        
        if(useCache) {
            List<PhenotypeEntityJoin> list = joinList_new.stream()
                    .filter(join -> join.getPhenotypePublicationJoins().stream().anyMatch(join1 -> join1.getModel() != null))
                    .filter(join -> join.getPhenotypePublicationJoins().stream().anyMatch(join1 -> join1.getModel().getPrimaryKey().equals("MGI:6272038")))
                    .collect(Collectors.toList());
        }
        
        List<PhenotypeAnnotation> allPhenotypeAnnotations_new = getPhenotypeAnnotations(joinList_new);
        // geneID, Map<phenotype, List<PhenotypeAnnotation>>
        startProcess("allPhenotypeAnnotations.groupingBy getPhenotype", allPhenotypeAnnotations_new.size());
        Map<String, Map<String, List<PhenotypeAnnotation>>> annotationMergeMap_new = allPhenotypeAnnotations_new.stream()
                .collect(groupingBy(phenotypeAnnotation -> phenotypeAnnotation.getGene().getPrimaryKey(), groupingBy(PhenotypeAnnotation::getPhenotype)));
        finishProcess();
        
        // merge annotations with the same phenotype
        Map<String, List<PhenotypeAnnotation>> phenotypeAnnotationMap_new = getMergedPhenotypeMap(annotationMergeMap_new);

        storeIntoCache(joinList_new, allPhenotypeAnnotations_new, phenotypeAnnotationMap_new, CacheAlliance.GENE_PHENOTYPE);
        
        allPhenotypeAnnotations_new.clear();
        annotationMergeMap_new.clear();
        phenotypeAnnotationMap_new.clear();

        startProcess("GenePhenotypeCacher.getAllelePhenotypeAnnotations");
        List<PhenotypeEntityJoin> joinListAllele_new = phenotypeRepository.getAllelePhenotypeAnnotations();
        finishProcess();
        
        // used to populate the DOTerm object on the PrimaryAnnotationEntity object
        List<PhenotypeAnnotation> allelePhenotypeAnnotations_new = getPhenotypeAnnotations(joinListAllele_new);

        // alleleID, Map<phenotype, List<PhenotypeAnnotation>>
        startProcess("allelePhenotypeAnnotations.groupingBy getPhenotype", allelePhenotypeAnnotations_new.size());
        Map<String, Map<String, List<PhenotypeAnnotation>>> annotationAlleleMergeMap_new = allelePhenotypeAnnotations_new.stream()
                .collect(groupingBy(phenotypeAnnotation -> phenotypeAnnotation.getAllele().getPrimaryKey(), groupingBy(PhenotypeAnnotation::getPhenotype)));
        finishProcess();
        
        // merge annotations with the same phenotype
        Map<String, List<PhenotypeAnnotation>> phenotypeAnnotationAlleleMap_new = getMergedPhenotypeMap(annotationAlleleMergeMap_new);


        storeIntoCache(joinList_new, allelePhenotypeAnnotations_new, phenotypeAnnotationAlleleMap_new, CacheAlliance.ALLELE_PHENOTYPE);
        
        phenotypeAnnotationAlleleMap_new.clear();
        allelePhenotypeAnnotations_new.clear();
        annotationAlleleMergeMap_new.clear();
        
        joinList_new.clear();
        
        
        startProcess("phenotypeRepository.getAllPhenotypeAnnotationsPureAGM");
        List<PhenotypeEntityJoin> pureAgmPhenotypes_new = phenotypeRepository.getAllPhenotypeAnnotationsPureAGM();
        log.info("Retrieved " + String.format("%,d", pureAgmPhenotypes_new.size()) + " PhenotypeEntityJoin records for pure AGMs");
        // set the gene object on the join
        finishProcess();

        // phenotypeEntityJoin PK, List<Gene>
        Map<String, List<Gene>> modelGenesMap_new = new HashMap<>();

        pureAgmPhenotypes_new.stream()
                .filter(join -> CollectionUtils.isNotEmpty(join.getModel().getAlleles()))
                .forEach(join -> {
                    Set<Gene> geneList = join.getModel().getAlleles().stream()
                            .map(Allele::getGene)
                            .collect(toSet());
                    final String primaryKey = join.getPrimaryKey();
                    List<Gene> genes = modelGenesMap_new.get(primaryKey);
                    if (genes == null) {
                        genes = new ArrayList<>();
                    }
                    genes.addAll(geneList);
                    genes = genes.stream().distinct().collect(toList());
                    modelGenesMap_new.put(primaryKey, genes);
                });
        pureAgmPhenotypes_new.stream()
                .filter(join -> CollectionUtils.isNotEmpty(join.getModel().getAlleles()))
                .forEach(join -> {
                    Set<Gene> geneList = join.getModel().getSequenceTargetingReagents().stream()
                            .map(SequenceTargetingReagent::getGene)
                            .collect(toSet());
                    final String primaryKey = join.getPrimaryKey();
                    List<Gene> genes = modelGenesMap_new.get(primaryKey);
                    if (genes == null) {
                        genes = new ArrayList<>();
                    }
                    genes.addAll(geneList);
                    genes = genes.stream().distinct().collect(toList());
                    modelGenesMap_new.put(primaryKey, genes);
                });

        List<PhenotypeAnnotation> allPhenotypeAnnotationsPure_new = pureAgmPhenotypes_new.stream()
                .map(join -> {
                    PhenotypeAnnotation document = new PhenotypeAnnotation();
                    final AffectedGenomicModel model = join.getModel();
                    document.setModel(model);
                    document.setPrimaryKey(join.getPrimaryKey());
                    document.setPhenotype(join.getPhenotype().getPhenotypeStatement());
                    document.setPublications(join.getPublications());

                    PrimaryAnnotatedEntity entity = new PrimaryAnnotatedEntity();
                    entity.setId(model.getPrimaryKey());
                    entity.setEntityJoinPk(join.getPrimaryKey());
                    entity.setName(model.getName());
                    entity.setDisplayName(model.getNameText());
                    entity.setUrl(model.getModCrossRefCompleteUrl());
                    entity.setType(GeneticEntity.CrossReferenceType.getCrossReferenceType(model.getSubtype()));
                    entity.addPublicationEvidenceCode(join.getPhenotypePublicationJoins());
                    entity.addPhenotype(join.getPhenotype().getPhenotypeStatement());
                    entity.setDataProvider(model.getDataProvider());
                    document.addPrimaryAnnotatedEntity(entity);
                    return document;
                })
                .collect(Collectors.toList());
        
        pureAgmPhenotypes_new.clear();
        

        Map<String, PhenotypeAnnotation> paMap_new = allPhenotypeAnnotationsPure_new.stream()
                .collect(toMap(PhenotypeAnnotation::getPrimaryKey, entity -> entity));
        
        allPhenotypeAnnotationsPure_new.clear();
        
        // merge annotations with the same model
        // geneID, Map<modelID, List<PhenotypeAnnotation>>>
/*
        Map<String, Map<String, List<PhenotypeAnnotation>>> annotationPureMergeMap = allPhenotypeAnnotationsPure.stream()
                .collect(groupingBy(phenotypeAnnotation -> phenotypeAnnotation.getGene().getPrimaryKey(), groupingBy(annotation -> annotation.getModel().getPrimaryKey())));
*/
        Map<String, Map<String, List<PhenotypeAnnotation>>> annotationPureMergeMap_new = new HashMap<>();

        modelGenesMap_new.forEach((phenotypeEntityJoinID, genes) -> {
            PhenotypeAnnotation phenoAnnot = paMap_new.get(phenotypeEntityJoinID);

            genes.forEach(gene -> {
                Map<String, List<PhenotypeAnnotation>> annotations = annotationPureMergeMap_new.get(gene.getPrimaryKey());
                if (annotations == null) {
                    annotations = new HashMap<>();
                    annotationPureMergeMap_new.put(gene.getPrimaryKey(), annotations);
                }

                List<PhenotypeAnnotation> phenos = annotations.get(phenoAnnot.getModel().getPrimaryKey());
                if (phenos == null) {
                    phenos = new ArrayList<>();
                    annotations.put(phenoAnnot.getModel().getPrimaryKey(), phenos);
                }
                phenos.add(phenoAnnot);
            });
        });
        
        modelGenesMap_new.clear();
        paMap_new.clear();


        Map<String, List<PrimaryAnnotatedEntity>> phenotypeAnnotationPureMap_new = new HashMap<>();
        
        annotationPureMergeMap_new.forEach((geneID, modelIdMap) -> modelIdMap.forEach((modelID, phenotypeAnnotations) -> {
            List<PrimaryAnnotatedEntity> mergedAnnotations = phenotypeAnnotationPureMap_new.get(geneID);
            if (mergedAnnotations == null)
                mergedAnnotations = new ArrayList<>();
            PrimaryAnnotatedEntity entity = phenotypeAnnotations.get(0).getPrimaryAnnotatedEntities().get(0);
            phenotypeAnnotations.forEach(phenotypeAnnotation -> {
                entity.addPhenotype(phenotypeAnnotation.getPhenotype());
                entity.addPublicationEvidenceCode(phenotypeAnnotation.getPrimaryAnnotatedEntities().get(0).getPublicationEvidenceCodes());
            });
            mergedAnnotations.add(entity);
            phenotypeAnnotationPureMap_new.put(geneID, mergedAnnotations);
        }));
        
        annotationPureMergeMap_new.clear();
        

        BasicCachingManager managerModel = new BasicCachingManager();

        startProcess("phenotypeAnnotationPureMap", phenotypeAnnotationPureMap_new.size());
        phenotypeAnnotationPureMap_new.forEach((geneID, value) -> {
            if (geneID.equals("MGI:104798")) {
                log.info("found gene: " + geneID + " with annotations: " + value.size());
            }
            managerModel.setCache(geneID, value, View.PrimaryAnnotation.class, CacheAlliance.GENE_PURE_AGM_PHENOTYPE);
            progressProcess();
        });
        
        phenotypeAnnotationPureMap_new.clear();
        
        finishProcess();
        phenotypeRepository.clearCache();
    }

    private void storeIntoCache(List<PhenotypeEntityJoin> joinList, List<PhenotypeAnnotation> allPhenotypeAnnotations, Map<String, List<PhenotypeAnnotation>> phenotypeAnnotationMap, CacheAlliance cacheSpace) {
        BasicCachingManager manager = new BasicCachingManager();
        startProcess(cacheSpace.name() + " into cache", phenotypeAnnotationMap.size());
        phenotypeAnnotationMap.forEach((key, value) -> {
            manager.setCache(key, value, View.PhenotypeAPI.class, cacheSpace);
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
                    final Allele allele = phenotypeEntityJoin.getAllele();
                    if (allele != null)
                        document.setAllele(allele);
                    document.setPhenotype(phenotypeEntityJoin.getPhenotype().getPhenotypeStatement());
                    document.setPublications(phenotypeEntityJoin.getPublications());
                    document.setSource(phenotypeEntityJoin.getSource());
                    Map<String, PrimaryAnnotatedEntity> entities = new HashMap<>();

                    // if AGMs are present
                    if (CollectionUtils.isNotEmpty(phenotypeEntityJoin.getPhenotypePublicationJoins())) {
                        boolean hasAGMs = phenotypeEntityJoin.getPhenotypePublicationJoins().stream()
                                .anyMatch(join -> join.getModel() != null);

                        if (hasAGMs) {
                            phenotypeEntityJoin.getPhenotypePublicationJoins()
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
                                        }
                                        entity.setDataProvider(phenotypeEntityJoin.getDataProvider());
                                        entity.addPublicationEvidenceCode(pubJoin);
                                        document.addPrimaryAnnotatedEntity(entity);
                                        entity.addPhenotype(phenotypeEntityJoin.getPhenotype().getPhenotypeStatement());
                                        entities.put(model.getPrimaryKey(), entity);
                                    });
                        }
                    }
                    progressProcess();
                    return document;
                })
                .collect(toList());
    }


}
