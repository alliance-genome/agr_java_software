package org.alliancegenome.cacher.cachers;

import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.manager.BasicCachingManager;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.repository.AlleleRepository;
import org.alliancegenome.neo4j.view.View;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

public class AlleleCacher extends Cacher {

    private static AlleleRepository alleleRepository = new AlleleRepository();

    @Override
    protected void cache() {

        Set<Allele> allAlleles = alleleRepository.getAllAlleles();

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
        setCacheStatus(allAlleles.size(), CacheAlliance.ALLELE.getCacheName());

        alleleRepository.clearCache();

    }

}
