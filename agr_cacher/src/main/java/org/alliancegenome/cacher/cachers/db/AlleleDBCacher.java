package org.alliancegenome.cacher.cachers.db;

import org.alliancegenome.cache.AllianceCacheManager;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cacher.cachers.Cacher;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.repository.AlleleRepository;
import org.ehcache.Cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static java.util.stream.Collectors.groupingBy;

public class AlleleDBCacher extends Cacher {

    private static AlleleRepository alleleRepository = new AlleleRepository();
    private static Set<Allele> allAlleles = null;

    @Override
    protected void cache() {

        allAlleles = alleleRepository.getAllAlleles();

        if (allAlleles == null)
            return;

        Map<String, List<Allele>> map = allAlleles.stream().collect(groupingBy(allele -> allele.getGene().getPrimaryKey()));

        Cache<String, ArrayList> cache = AllianceCacheManager.getCacheSpace(CacheAlliance.ALLELE);
        for (Map.Entry<String, List<Allele>> entry : map.entrySet()) {
            cache.put(entry.getKey(), new ArrayList(entry.getValue()));
        }

        alleleRepository.clearCache();
    }

}
