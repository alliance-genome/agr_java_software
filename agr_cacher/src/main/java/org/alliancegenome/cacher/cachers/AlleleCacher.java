package org.alliancegenome.cacher.cachers;

import static java.util.stream.Collectors.groupingBy;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.manager.AlleleAllianceCacheManager;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.neo4j.entity.node.Allele;
import org.alliancegenome.neo4j.repository.AlleleRepository;
import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.core.JsonProcessingException;

public class AlleleCacher extends Cacher {

    private static AlleleRepository alleleRepository = new AlleleRepository();

    @Override
    protected void cache() {

        Set<Allele> allAlleles = alleleRepository.getAllAlleles();

        if (allAlleles == null)
            return;

        Map<String, List<Allele>> map = allAlleles.stream().collect(groupingBy(allele -> allele.getGene().getPrimaryKey()));

        AlleleAllianceCacheManager manager = new AlleleAllianceCacheManager();
        for (Map.Entry<String, List<Allele>> entry : map.entrySet()) {
            JsonResultResponse<Allele> result = new JsonResultResponse<>();
            result.setResults(entry.getValue());
            try {
                manager.putCache(entry.getKey(), result, View.GeneAllelesAPI.class, CacheAlliance.ALLELE);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
        }

        alleleRepository.clearCache();
        
    }

}
