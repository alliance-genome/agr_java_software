package org.alliancegenome.cacher.cachers;

import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.log4j.Log4j2;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.manager.PhenotypeCacheManager;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.neo4j.entity.PhenotypeAnnotation;
import org.alliancegenome.neo4j.entity.PrimaryAnnotatedEntity;
import org.alliancegenome.neo4j.entity.node.AffectedGenomicModel;
import org.alliancegenome.neo4j.entity.node.Gene;
import org.alliancegenome.neo4j.entity.node.GeneticEntity;
import org.alliancegenome.neo4j.entity.node.PhenotypeEntityJoin;
import org.alliancegenome.neo4j.repository.PhenotypeRepository;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

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

        int size = joinList.size();
        log.info("Retrieved " + String.format("%,d", size) + " PhenotypeEntityJoin records");
        startProcess("allPhenotypeAnnotations", size);

        // used to populate the DOTerm object on the PrimaryAnnotationEntity object
        List<PhenotypeAnnotation> allPhenotypeAnnotations = joinList.stream()
                .map(phenotypeEntityJoin -> {
                    PhenotypeAnnotation document = new PhenotypeAnnotation();
                    final Gene gene = phenotypeEntityJoin.getGene();
                    document.setGene(gene);
                    if (phenotypeEntityJoin.getAllele() != null)
                        document.setAllele(phenotypeEntityJoin.getAllele());
                    document.setPhenotype(phenotypeEntityJoin.getPhenotype().getPhenotypeStatement());
                    document.setPublications(phenotypeEntityJoin.getPublications());
                    // if AGMs are present
                    if (CollectionUtils.isNotEmpty(phenotypeEntityJoin.getPhenotypePublicationJoins())) {
                        phenotypeEntityJoin.getPhenotypePublicationJoins()
                                .stream()
                                .filter(pubJoin -> pubJoin.getModel() != null)
                                .forEach(pubJoin -> {
                                    AffectedGenomicModel model = pubJoin.getModel();
                                    PrimaryAnnotatedEntity entity = new PrimaryAnnotatedEntity();
                                    entity.setId(model.getPrimaryKey());
                                    entity.setName(model.getName());
                                    entity.setUrl(model.getModCrossRefCompleteUrl());
                                    entity.setDisplayName(model.getNameText());
                                    entity.setType(GeneticEntity.getType(model.getSubtype()));
                                    entity.setDataProvider(phenotypeEntityJoin.getDataProvider());
                                    entity.addPublicationEvidenceCode(pubJoin);
                                    document.addPrimaryAnnotatedEntity(entity);
                                    entity.addPhenotype(phenotypeEntityJoin.getPhenotype().getPhenotypeStatement());
                                });
                        // if Gene-only create a new PAE of type 'Gene'
                        if (gene != null) {
                            PrimaryAnnotatedEntity entity = new PrimaryAnnotatedEntity();
                            entity.setId(gene.getPrimaryKey());
                            entity.setName(gene.getSymbol());
                            entity.setDisplayName(gene.getSymbol());
                            entity.setType(GeneticEntity.getType("gene"));
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

        phenotypeRepository.clearCache();
    }


}
