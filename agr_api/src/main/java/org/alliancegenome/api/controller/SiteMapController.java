package org.alliancegenome.api.controller;

import java.util.*;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;

import org.alliancegenome.api.model.xml.*;
import org.alliancegenome.api.rest.interfaces.SiteMapRESTInterface;
import org.alliancegenome.cache.repository.SiteMapCacheManager;
import org.alliancegenome.core.config.ConfigHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RequestScoped
public class SiteMapController implements SiteMapRESTInterface {

	@Inject SiteMapCacheManager manager;

	@Override
	public SiteMapIndex getSiteMap() {
		
		List<SiteMap> list = new ArrayList<SiteMap>();
		
		List<String> geneKeys = manager.getGenesKeys();
		log.info("Gene Keys: "	+ geneKeys.size());
		for(String s: geneKeys) {
			list.add(new SiteMap(buildUrl("api/sitemap/gene-sitemap-" + s + ".xml"), ConfigHelper.getAppStart()));
		}
		
		List<String> diseaseKeys = manager.getDiseaseKeys();
		log.info("Disease Keys: "  + diseaseKeys.size());
		for(String s: diseaseKeys) {
			list.add(new SiteMap(buildUrl("api/sitemap/disease-sitemap-" + s + ".xml"), ConfigHelper.getAppStart()));
		}
		
		List<String> alleleKeys = manager.getAlleleKeys();
		log.info("Disease Keys: "  + alleleKeys.size());
		for(String s: alleleKeys) {
			list.add(new SiteMap(buildUrl("api/sitemap/allele-sitemap-" + s + ".xml"), ConfigHelper.getAppStart()));
		}
		
		SiteMapIndex index = new SiteMapIndex();
		index.setSitemap(list);
		return index;
	}

	@Override
	public XMLURLSet getCategorySiteMap(String category, Integer page) {
		return buildSiteMapByCategory(category, page);
	}


	private XMLURLSet buildSiteMapByCategory(String category, Integer page) {

		if(category.equals("gene")) {
			List<XMLURL> list = manager.getGenes(page.toString());
			XMLURLSet set = new XMLURLSet();
			set.setUrl(list);
			for(XMLURL url: list) {
				url.setLoc(buildUrl(url.getLoc()));
			}
			return set;
		}
		
		if(category.equals("disease")) {
			List<XMLURL> list = manager.getDiseases(page.toString());
			XMLURLSet set = new XMLURLSet();
			set.setUrl(list);
			for(XMLURL url: list) {
				url.setLoc(buildUrl(url.getLoc()));
			}
			return set;
		}
		
		if(category.equals("allele")) {
			List<XMLURL> list = manager.getAlleles(page.toString());
			XMLURLSet set = new XMLURLSet();
			set.setUrl(list);
			for(XMLURL url: list) {
				url.setLoc(buildUrl(url.getLoc()));
			}
			return set;
		}

		return null;
	}

	private String buildUrl(String inUrl) {
		final StringBuilder url = new StringBuilder();
		url.append("https://www.alliancegenome.org");

		if(inUrl != null) {
			url.append("/");
			url.append(inUrl);
		}
		return url.toString();
	}

}
