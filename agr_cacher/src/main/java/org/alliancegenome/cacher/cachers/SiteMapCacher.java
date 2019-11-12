package org.alliancegenome.cacher.cachers;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.manager.SiteMapCacheManager;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.neo4j.repository.DiseaseRepository;
import org.alliancegenome.neo4j.repository.GeneRepository;
import org.alliancegenome.neo4j.view.View;

import com.fasterxml.jackson.core.JsonProcessingException;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class SiteMapCacher extends Cacher {
    
    private Integer batchSize = 15000;
    private GeneRepository geneRepository = new GeneRepository();
    //private AlleleRepository alleleRepository = new AlleleRepository();
    private DiseaseRepository diseaseRepository = new DiseaseRepository();
    private SiteMapCacheManager manager = new SiteMapCacheManager();
    
    @Override
    protected void cache() {
        
        startProcess("geneRepository.getAllGeneKeys");
        List<String> geneKeyList = geneRepository.getAllGeneKeys();
        log.debug("Gene List Size: " + geneKeyList.size());
        cacheSiteMap(geneKeyList, CacheAlliance.GENE_SITEMAP);
        finishProcess();
        

        startProcess("diseaseRepository.getAllDiseaseWithAnnotationsKeys");
        Set<String> diseaseKeyList = diseaseRepository.getAllDiseaseWithAnnotationsKeys();
        log.debug("Disease List Size: " + diseaseKeyList.size());
        cacheSiteMap(diseaseKeyList, CacheAlliance.DISEASE_SITEMAP);
        finishProcess();
        
    }
    
    private void cacheSiteMap(Iterable<String> list, CacheAlliance cache) {
        List<String> idList = new ArrayList<String>();
        int c = 0;
        for(String id: list) {
            idList.add(id);
            if(idList.size() >= batchSize) {
                JsonResultResponse<String> result = new JsonResultResponse<>();
                result.setResults(new ArrayList<>(idList));
                try {
                    manager.putCache(String.valueOf(c), result, View.Default.class, cache);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                idList.clear();
                c++;
            }
        }

        if(idList.size() > 0) {
            JsonResultResponse<String> result = new JsonResultResponse<>();
            result.setResults(new ArrayList<>(idList));
            try {
                manager.putCache(String.valueOf(c), result, View.Default.class, cache);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }
            idList.clear();
        }
        
    }

}
