package org.alliancegenome.cache.repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.model.xml.XMLURL;
import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.CacheService;
import org.alliancegenome.core.config.ConfigHelper;

import lombok.extern.log4j.Log4j2;

@Log4j2
@RequestScoped
public class SiteMapCacheManager {

	@Inject
	private CacheService cacheService;

	public List<XMLURL> getGenes(String key) {

		List<String> allIds = getCacheEntry(key, CacheAlliance.SITEMAP_GENE);

		List<XMLURL> urls = new ArrayList<XMLURL>();

		for (String id : allIds) {
			Date date = null;

			//if(hit.getSource().get("dateProduced") != null) {
			//	date = new Date((long)hit.getSource().get("dateProduced"));
			//}

			urls.add(new XMLURL("gene/" + id, ConfigHelper.getAppStart(), "monthly", "0.6"));
		}

		return urls;
	}

	public List<XMLURL> getDiseases(String key) {

		List<String> allIds = getCacheEntry(key, CacheAlliance.SITEMAP_DISEASE);

		List<XMLURL> urls = new ArrayList<XMLURL>();

		for (String id : allIds) {
			Date date = null;

			//if(hit.getSource().get("dateProduced") != null) {
			//	date = new Date((long)hit.getSource().get("dateProduced"));
			//}

			urls.add(new XMLURL("disease/" + id, ConfigHelper.getAppStart(), "monthly", "0.6"));
		}

		return urls;
	}


	public List<String> getGenesKeys() {
		return cacheService.getAllKeys(CacheAlliance.SITEMAP_GENE);
	}

	public List<String> getDiseaseKeys() {
		return cacheService.getAllKeys(CacheAlliance.SITEMAP_DISEASE);
	}
	
	public List<String> getAlleleKeys() {
		return cacheService.getAllKeys(CacheAlliance.SITEMAP_ALLELE);
	}

	public List<String> getCacheEntry(String entityID, CacheAlliance cacheSpace) {
		return cacheService.getCacheEntries(entityID, cacheSpace);
	}

}
