package org.alliancegenome.cacher.cachers;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.entity.AlleleVariantSequence;
import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.entity.node.GeneticEntity;
import org.alliancegenome.neo4j.entity.node.Species;
import org.alliancegenome.neo4j.repository.AlleleRepository;
import org.alliancegenome.neo4j.view.View;
import org.apache.commons.collections.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Log4j2
public class AlleleCacher extends Cacher {

    private static AlleleRepository alleleRepository = new AlleleRepository();

    @Override
    protected void cache() {

        startProcess("get All Alleles");
        Set<Allele> allAlleles = alleleRepository.getAllAlleles();
        if (allAlleles == null)
            return;
        log.info("Number of Alleles: " + String.format("%,d", allAlleles.size()));
        // group by genes. This ignores alleles without gene associations
        Map<String, List<Allele>> map = allAlleles.stream()
                .filter(allele -> allele.getGene() != null)
                .collect(groupingBy(allele -> allele.getGene().getPrimaryKey()));

        allAlleles.forEach(allele -> allele.setPhenotypes(allele.getPhenotypes().stream()
                .sorted(Comparator.comparing(phenotype -> phenotype.getPhenotypeStatement().toLowerCase()))
                .collect(Collectors.toList())));
        finishProcess();

        populateCacheFromMap(map, View.GeneAllelesAPI.class, CacheAlliance.ALLELE_GENE);

        CacheStatus status = new CacheStatus(CacheAlliance.ALLELE_GENE);
        status.setNumberOfEntities(allAlleles.size());

        Map<String, List<Species>> speciesStats = allAlleles.stream()
                .map(GeneticEntity::getSpecies)
                .collect(groupingBy(Species::getName));

        Map<String, Integer> entityStats = new TreeMap<>();
        map.forEach((geneID, alleles) -> entityStats.put(geneID, alleles.size()));

        populateStatisticsOnStatus(status, entityStats, speciesStats);
        status.setCollectionEntity(Allele.class.getSimpleName());
        status.setJsonViewClass(View.GeneAllelesAPI.class.getSimpleName());
        setCacheStatus(status);

        // generate Allele detail records

        List<AlleleVariantSequence> alleleVariantSequences = allAlleles.stream()
                .map(allele -> {
                    if (CollectionUtils.isEmpty(allele.getVariants())) {
                        return List.of(new AlleleVariantSequence(allele, null, null));
                    } else {
                        return allele.getVariants().stream()
                                .map(variant -> {
                                    if (CollectionUtils.isEmpty(variant.getTranscriptLevelConsequence())) {
                                        return List.of(new AlleleVariantSequence(allele, variant, null));
                                    } else {
                                        return variant.getTranscriptLevelConsequence().stream()
                                                .map(transcriptLevelConsequence -> new AlleleVariantSequence(allele, variant, transcriptLevelConsequence))
                                                .collect(Collectors.toList());
                                    }
                                })
                                .flatMap(Collection::stream)
                                .collect(Collectors.toList());
                    }
                })
                .flatMap(Collection::stream)
                .collect(Collectors.toList());

//        alleleVariantSequences = alleleVariantSequences.stream().filter(sequence -> sequence.getAllele().getPrimaryKey().equals("ZFIN:ZDB-ALT-130411-1942")).collect(Collectors.toList());

        Map<String, List<AlleleVariantSequence>> allRecordsMap = alleleVariantSequences.stream()
                .filter(sequence -> sequence.getAllele().getGene() != null)
                .collect(groupingBy(sequence -> sequence.getAllele().getGene().getPrimaryKey()));

        populateCacheFromMap(allRecordsMap, View.GeneAlleleVariantSequenceAPI.class, CacheAlliance.ALLELE_VARIANT_SEQUENCE_GENE);

/*
        status = new CacheStatus(CacheAlliance.ALLELE_VARIANT_SEQUENCE_GENE);
        status.setNumberOfEntities(alleleVariantSequences.size());

        speciesStats = alleleVariantSequences.stream()
                .map(AlleleVariantSequence::getAllele)
                .map(GeneticEntity::getSpecies)
                .collect(groupingBy(Species::getName));

        Map<String, Integer> entityStatsDetail = new TreeMap<>();
        speciesStats.forEach((geneID, alleles) -> entityStatsDetail.put(geneID, alleles.size()));

        populateStatisticsOnStatus(status, entityStatsDetail, speciesStats);
        status.setCollectionEntity(Allele.class.getSimpleName());
        status.setJsonViewClass(View.GeneAllelesAPI.class.getSimpleName());
        setCacheStatus(status);
*/

        // create allele-species index
        // <taxonID, List<Allele>>
        // include alleles without gene associations
        Map<String, List<Allele>> speciesMap = allAlleles.stream()
                .collect(groupingBy(allele -> allele.getSpecies().getPrimaryKey()));
        populateCacheFromMap(speciesMap, View.GeneAlleleVariantSequenceAPI.class, CacheAlliance.ALLELE_VARIANT_SEQUENCE_GENE);

        alleleRepository.clearCache();

    }


}
