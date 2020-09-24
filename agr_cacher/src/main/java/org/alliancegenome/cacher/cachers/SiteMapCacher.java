package org.alliancegenome.cacher.cachers;

import java.util.*;

import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.repository.helper.JsonResultResponse;
import org.alliancegenome.neo4j.repository.*;
import org.alliancegenome.neo4j.view.View;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class SiteMapCacher extends Cacher {
    
    private Integer batchSize = 15000;
    private GeneRepository geneRepository = new GeneRepository();
    private AlleleRepository alleleRepository = new AlleleRepository();
    private DiseaseRepository diseaseRepository = new DiseaseRepository();

    @Override
    protected void cache() {
        
        startProcess("geneRepository.getAllGeneKeys");
        List<String> geneKeyList = geneRepository.getAllGeneKeys();
        log.debug("Gene List Size: " + geneKeyList.size());
        cacheSiteMap(geneKeyList, CacheAlliance.SITEMAP_GENE);
        finishProcess();
        
        List<String> alleleKeyList = alleleRepository.getAllAlleleKeys();
        log.debug("Allele List Size: " + alleleKeyList.size());
        cacheSiteMap(alleleKeyList, CacheAlliance.SITEMAP_ALLELE);
        finishProcess();

        startProcess("diseaseRepository.getAllDiseaseWithAnnotationsKeys");
        Set<String> diseaseKeyList = diseaseRepository.getAllDiseaseWithAnnotationsKeys();
        log.debug("Disease List Size: " + diseaseKeyList.size());
        cacheSiteMap(diseaseKeyList, CacheAlliance.SITEMAP_DISEASE);
        finishProcess();
        
    }
    
    private void cacheSiteMap(Iterable<String> list, CacheAlliance cache) {
        List<String> idList = new ArrayList<>();
        int c = 0;
        for(String id: list) {
            idList.add(id);
            if(idList.size() >= batchSize) {
                cacheService.putCacheEntry(String.valueOf(c), idList, View.Default.class, cache);
                idList.clear();
                c++;
            }
        }

        if(idList.size() > 0) {
            JsonResultResponse<String> result = new JsonResultResponse<>();
            result.setResults(new ArrayList<>(idList));
            cacheService.putCacheEntry(String.valueOf(c), idList, View.Default.class, cache);
            idList.clear();
        }
        
    }

}
