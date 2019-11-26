package org.alliancegenome.cacher.cachers;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.manager.BasicCachingManager;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.repository.AlleleRepository;
import org.alliancegenome.neo4j.view.View;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Log4j2
public class AlleleCacher extends Cacher {

    private static AlleleRepository alleleRepository = new AlleleRepository();

    @Override
    protected void cache() {

        Set<Allele> allAlleles = alleleRepository.getAllAlleles();
        log.info("Number of Alleles: " + String.format("%,d", allAlleles.size()));

        if (allAlleles == null)
            return;

        Map<String, List<Allele>> map = allAlleles.stream().collect(groupingBy(allele -> allele.getGene().getPrimaryKey()));

        allAlleles.forEach(allele -> allele.setPhenotypes(allele.getPhenotypes().stream()
                .sorted(Comparator.comparing(phenotype -> phenotype.getPhenotypeStatement().toLowerCase()))
                .collect(Collectors.toList())));

        BasicCachingManager manager = new BasicCachingManager();
        for (Map.Entry<String, List<Allele>> entry : map.entrySet()) {
            manager.setCache(entry.getKey(), entry.getValue(), View.GeneAllelesAPI.class, CacheAlliance.ALLELE);
        }

        CacheStatus status = new CacheStatus(CacheAlliance.ALLELE.getCacheName());
        status.setNumberOfEntities(allAlleles.size());

        Map<String, List<Allele>> speciesStats = allAlleles.stream().collect(groupingBy(allele -> allele.getGene().getSpecies().getName()));

        Map<String, Integer> stats = new HashMap<>(map.size());
        map.forEach((geneID, alleles) -> {
            stats.put(geneID, alleles.size());
        });

        Arrays.stream(SpeciesType.values())
                .filter(speciesType -> !speciesStats.keySet().contains(speciesType.getName()))
                .forEach(speciesType -> speciesStats.put(speciesType.getName(), new ArrayList<>()));

        Map<String, Integer> speciesStatsInt = new HashMap<>();
        speciesStats.forEach((species, alleles) -> {
            speciesStatsInt.put(species, alleles.size());
        });

        status.setEntityStats(stats);
        status.setSpeciesStats(speciesStatsInt);
        setCacheStatus(status);

        alleleRepository.clearCache();

    }

}
