package org.alliancegenome.cacher.cachers;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.manager.BasicCacheManager;
import org.alliancegenome.neo4j.entity.node.ECOTerm;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.alliancegenome.neo4j.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Log4j2
public class EcoCodeHelperCacher extends Cacher {

    private static DiseaseRepository diseaseRepository = new DiseaseRepository();

    @Override
    protected void cache() {

        // dej primary key, list of ECO terms
        Map<String, List<ECOTerm>> allEcos = diseaseRepository.getEcoTermMap();

        BasicCacheManager<String> basicManager = new BasicCacheManager<>();
        final Class<View.DiseaseCacher> classView = View.DiseaseCacher.class;

        allEcos.forEach((key, ecoTerms) -> {
            basicManager.setCache(key, ecoTerms, classView, CacheAlliance.ECO_MAP);
        });
        log.info("Retrieved " + String.format("%,d", allEcos.size()) + " EcoTerm mappings");

        Map<String, Set<String>> closure = diseaseRepository.getClosureChildToParentsMapping();

        closure.forEach((parent, children) -> {
            basicManager.setCache(parent, new ArrayList(children), classView, CacheAlliance.CLOSURE_MAP);
        });

        log.info("Retrieved " + String.format("%,d", closure.size()) + " closure parents");

    }

}
// 2524b083-7459-4e90-907d-230305b247fd