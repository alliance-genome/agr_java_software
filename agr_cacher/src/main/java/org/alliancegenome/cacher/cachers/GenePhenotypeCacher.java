package org.alliancegenome.cacher.cachers;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.log4j.Log4j2;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.manager.ModelAllianceCacheManager;
import org.alliancegenome.cache.manager.PhenotypeCacheManager;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
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
        startProcess("phenotypeRepository.getAllPhenotypeAnnotations");

        List<PhenotypeEntityJoin> joinList = phenotypeRepository.getAllPhenotypeAnnotations();

        finishProcess();


        List<PhenotypeEntityJoin> list = joinList.stream()
                .filter(join -> join.getPhenotypePublicationJoins().stream().anyMatch(join1 -> join1.getModel() != null))
                .filter(join -> join.getPhenotypePublicationJoins().stream().anyMatch(join1 -> join1.getModel().getPrimaryKey().equals("MGI:6272038")))
                .collect(Collectors.toList());


        int size = joinList.size();
        log.info("Retrieved " + String.format("%,d", size) + " PhenotypeEntityJoin records");
        startProcess("allPhenotypeAnnotations", size);

        // used to populate the DOTerm object on the PrimaryAnnotationEntity object
        List<PhenotypeAnnotation> allPhenotypeAnnotations = joinList.stream()
                .map(phenotypeEntityJoin -> {
                    PhenotypeAnnotation document = new PhenotypeAnnotation();
                    final Gene gene = phenotypeEntityJoin.getGene();
                    document.setGene(gene);
                    final Allele allele = phenotypeEntityJoin.getAllele();
                    if (allele != null)
                        document.setAllele(allele);
                    document.setPhenotype(phenotypeEntityJoin.getPhenotype().getPhenotypeStatement());
                    document.setPublications(phenotypeEntityJoin.getPublications());
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
                        } else {
                            PrimaryAnnotatedEntity entity = new PrimaryAnnotatedEntity();
                            if (allele != null) {
                                entity.setId(allele.getPrimaryKey());
                                entity.setName(allele.getSymbol());
                                entity.setDisplayName(allele.getSymbolText());
                                entity.setType(GeneticEntity.CrossReferenceType.ALLELE);
                            }
                            // if Gene-only create a new PAE of type 'Gene'
                            else if (gene != null) {
                                entity.setId(gene.getPrimaryKey());
                                entity.setName(gene.getSymbol());
                                entity.setDisplayName(gene.getSymbol());
                                entity.setType(GeneticEntity.CrossReferenceType.GENE);
                            }
                            entity.setPublicationEvidenceCodes(phenotypeEntityJoin.getPhenotypePublicationJoins());
                            document.addPrimaryAnnotatedEntity(entity);
                        }
                    }
                    progressProcess();
                    return document;
                })
                .collect(toList());

        finishProcess();

        // merge annotations with the same phenotype
        // geneID, Map<phenotype, List<PhenotypeAnnotation>>
        Map<String, Map<String, List<PhenotypeAnnotation>>> annotationMergeMap = allPhenotypeAnnotations.stream()
                .collect(groupingBy(phenotypeAnnotation -> phenotypeAnnotation.getGene().getPrimaryKey(), groupingBy(PhenotypeAnnotation::getPhenotype)));

        Map<String, List<PhenotypeAnnotation>> phenotypeAnnotationMap = new HashMap<>();
        annotationMergeMap.forEach((geneID, value) -> {
            List<PhenotypeAnnotation> mergedAnnotations = new ArrayList<>();
            value.forEach((phenotype, phenotypeAnnotations) -> {
                // get first element and put all info from other collection elements.
                PhenotypeAnnotation entity = phenotypeAnnotations.get(0);
                phenotypeAnnotations.stream()
                        .filter(phenotypeAnnotation -> CollectionUtils.isNotEmpty(phenotypeAnnotation.getPrimaryAnnotatedEntities()))
                        .forEach(annotation -> entity.addPrimaryAnnotatedEntities(annotation.getPrimaryAnnotatedEntities()));
                mergedAnnotations.add(entity);
            });
            phenotypeAnnotationMap.put(geneID, mergedAnnotations);
        });

/*
        // group by gene IDs
        Map<String, List<PhenotypeAnnotation>> phenotypeAnnotationMap = mergedAnnotations.stream()
                .collect(groupingBy(phenotypeAnnotation -> phenotypeAnnotation.getGene().getPrimaryKey()));
*/

        PhenotypeCacheManager manager = new PhenotypeCacheManager();

        startProcess("phenotypeAnnotationMap into cache", phenotypeAnnotationMap.size());

        phenotypeAnnotationMap.forEach((key, value) -> {
            JsonResultResponse<PhenotypeAnnotation> result = new JsonResultResponse<>();
            result.setResults(value);
            try {
                manager.putCache(key, result, View.PhenotypeAPI.class, CacheAlliance.GENE_PHENOTYPE);
                progressProcess();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });
        finishProcess();

        List<PhenotypeEntityJoin> pureAgmPhenotypes = phenotypeRepository.getAllPhenotypeAnnotationsPureAGM();
        log.info("Retrieved " + String.format("%,d", pureAgmPhenotypes.size()) + " PhenotypeEntityJoin records for pure AGMs");
        // set the gene object on the join


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
                .filter(join -> CollectionUtils.isNotEmpty(join.getModel().getAlleles()))
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

        Map<String, PhenotypeAnnotation> paMap = allPhenotypeAnnotationsPure.stream()
                .collect(toMap(PhenotypeAnnotation::getPrimaryKey, entity -> entity));
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


        Map<String, List<PrimaryAnnotatedEntity>> phenotypeAnnotationPureMap = new HashMap<>();
        annotationPureMergeMap.forEach((geneID, modelIdMap) -> modelIdMap.forEach((modelID, phenotypeAnnotations) -> {
            List<PrimaryAnnotatedEntity> mergedAnnotations = phenotypeAnnotationPureMap.get(geneID);
            if (mergedAnnotations == null)
                mergedAnnotations = new ArrayList<>();
            PrimaryAnnotatedEntity entity = phenotypeAnnotations.get(0).getPrimaryAnnotatedEntities().get(0);
            phenotypeAnnotations.forEach(phenotypeAnnotation -> {
                entity.addPhenotype(phenotypeAnnotation.getPhenotype());
                entity.addPublicationEvidenceCode(phenotypeAnnotation.getPrimaryAnnotatedEntities().get(0).getPublicationEvidenceCodes());
            });
            mergedAnnotations.add(entity);
            phenotypeAnnotationPureMap.put(geneID, mergedAnnotations);
        }));

        ModelAllianceCacheManager managerModel = new ModelAllianceCacheManager();

        phenotypeAnnotationPureMap.forEach((geneID, value) -> {
            JsonResultResponse<PrimaryAnnotatedEntity> result = new JsonResultResponse<>();
            result.setResults(value);
            try {
                if (geneID.equals("MGI:104798")) {
                    log.info("found gene: " + geneID + " with annotations: " + result.getResults().size());
                    //result.getResults().forEach(entity -> log.info(entity.getId()));
                }
                managerModel.putCache(geneID, result, View.PrimaryAnnotation.class, CacheAlliance.GENE_PURE_AGM_PHENOTYPE);
                progressProcess();
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        });

        phenotypeRepository.clearCache();
    }


}
