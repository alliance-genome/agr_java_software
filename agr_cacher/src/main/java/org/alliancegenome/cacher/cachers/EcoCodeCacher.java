package org.alliancegenome.cacher.cachers;

import java.util.*;

import org.alliancegenome.api.entity.CacheStatus;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.neo4j.entity.node.ECOTerm;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.alliancegenome.neo4j.view.View;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class EcoCodeCacher extends Cacher {

    private static DiseaseRepository diseaseRepository = new DiseaseRepository();

    @Override
    protected void cache() {

        // dej primary key, list of ECO terms
        Map<String, List<ECOTerm>> allEcos = diseaseRepository.getEcoTermMap();
        
        final Class<View.DiseaseCacher> classView = View.DiseaseCacher.class;

        allEcos.forEach((key, ecoTerms) -> cacheService.putCacheEntry(key, ecoTerms, classView, CacheAlliance.ECO_MAP));
        log.info("Retrieved " + String.format("%,d", allEcos.size()) + " EcoTerm mappings");

        CacheStatus status = new CacheStatus(CacheAlliance.ECO_MAP);
        status.setNumberOfEntities(allEcos.size());
        setCacheStatus(status);

    }

}
// 2524b083-7459-4e90-907d-230305b247fd