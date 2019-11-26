package org.alliancegenome.cache.manager;

import lombok.extern.log4j.Log4j2;
import org.alliancegenome.api.model.xml.XMLURL;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.core.config.ConfigHelper;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Log4j2
public class SiteMapCacheManager extends BasicCachingManager<String> {


    public List<XMLURL> getGenes(String key) {

        List<String> allIds = getCacheEntry(key, CacheAlliance.GENE_SITEMAP);

        List<XMLURL> urls = new ArrayList<XMLURL>();

        for (String id : allIds) {
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

        for (String id : allIds) {
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
        return getCache(entityID, cacheSpace);
    }


}