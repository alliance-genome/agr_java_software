package org.alliancegenome.cacher.cachers;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.neo4j.entity.SpeciesType;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.repository.AlleleRepository;
import org.alliancegenome.neo4j.view.View;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Map.Entry.comparingByValue;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toMap;

@Log4j2
public class AlleleCacher extends Cacher {

    private static AlleleRepository alleleRepository = new AlleleRepository();

    @Override
    protected void cache() {

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

        populateCacheFromMap(map, View.GeneAllelesAPI.class, CacheAlliance.ALLELE_GENE);

        CacheStatus status = new CacheStatus(CacheAlliance.ALLELE_GENE);
        status.setNumberOfEntities(allAlleles.size());

        Map<String, List<Allele>> speciesStats = allAlleles.stream().collect(groupingBy(allele -> allele.getSpecies().getName()));

        Map<String, Integer> stats = new TreeMap<>();
        map.forEach((geneID, alleles) -> stats.put(geneID, alleles.size()));

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
        status.setCollectionEntity(Allele.class.getSimpleName());
        status.setJsonViewClass(View.GeneAllelesAPI.class.getSimpleName());
        setCacheStatus(status);

        // create allele-species index
        // <taxonID, List<Allele>>
        // include alleles without gene associations
        Map<String, List<Allele>> speciesMap = allAlleles.stream()
                .collect(groupingBy(allele -> allele.getSpecies().getPrimaryKey()));
        populateCacheFromMap(speciesMap, View.GeneAllelesAPI.class, CacheAlliance.ALLELE_SPECIES);

        alleleRepository.clearCache();

    }


}
