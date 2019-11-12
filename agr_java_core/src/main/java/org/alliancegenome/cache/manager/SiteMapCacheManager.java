package org.alliancegenome.cache.manager;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.alliancegenome.api.model.xml.XMLURL;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.service.JsonResultListStringResponse;
import org.alliancegenome.core.service.JsonResultResponse;
import org.alliancegenome.neo4j.view.View;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class SiteMapCacheManager extends CacheManager<String, JsonResultResponse<String>> {

    
    public List<XMLURL> getGenes(String key) {
        
        List<String> allIds = getCacheEntry(key, CacheAlliance.GENE_SITEMAP);

        List<XMLURL> urls = new ArrayList<XMLURL>();

        for(String id: allIds) {
            Date date = null;

            //System.out.println(hit.getFields());

            //if(hit.getSource().get("dateProduced") != null) {
            //  date = new Date((long)hit.getSource().get("dateProduced"));
            //}

            urls.add(new XMLURL("gene/" + id, ConfigHelper.getAppStart(), "monthly", "0.6"));
        }

        return urls;
    }
    
    public List<XMLURL> getDiseases(String key) {
        
        List<String> allIds = getCacheEntry(key, CacheAlliance.DISEASE_SITEMAP);

        List<XMLURL> urls = new ArrayList<XMLURL>();

        for(String id: allIds) {
            Date date = null;

            //System.out.println(hit.getFields());

            //if(hit.getSource().get("dateProduced") != null) {
            //  date = new Date((long)hit.getSource().get("dateProduced"));
            //}

            urls.add(new XMLURL("disease/" + id, ConfigHelper.getAppStart(), "monthly", "0.6"));
        }

        return urls;
    }
    

    public List<String> getGenesKeys() {
        return getAllKeys(CacheAlliance.GENE_SITEMAP);
    }
    
    public List<String> getDiseaseKeys() {
        return getAllKeys(CacheAlliance.DISEASE_SITEMAP);
    }
    
    public List<String> getCacheEntry(String entityID, CacheAlliance cacheSpace) {
        return getResultList(entityID, View.Default.class, JsonResultListStringResponse.class, cacheSpace);
    }


}