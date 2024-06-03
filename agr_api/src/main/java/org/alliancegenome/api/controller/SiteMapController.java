package org.alliancegenome.api.controller;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.api.model.xml.SiteMap;
import org.alliancegenome.api.model.xml.SiteMapIndex;
import org.alliancegenome.api.model.xml.XMLURL;
import org.alliancegenome.api.model.xml.XMLURLSet;
import org.alliancegenome.api.rest.interfaces.SiteMapRESTInterface;
import org.alliancegenome.cache.repository.SiteMapCacheManager;
import org.alliancegenome.core.config.ConfigHelper;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@RequestScoped
public class SiteMapController implements SiteMapRESTInterface {

	@Inject SiteMapCacheManager manager;

	@Override
	public SiteMapIndex getSiteMap() {

		List<SiteMap> list = new ArrayList<SiteMap>();

		List<String> geneKeys = manager.getGenesKeys();
		Log.info("Gene Keys: " + geneKeys.size());
		for (String s : geneKeys) {
			list.add(new SiteMap(buildUrl("api/sitemap/gene-sitemap-" + s + ".xml"), ConfigHelper.getAppStart()));
		}

		List<String> diseaseKeys = manager.getDiseaseKeys();
		Log.info("Disease Keys: " + diseaseKeys.size());
		for (String s : diseaseKeys) {
			list.add(new SiteMap(buildUrl("api/sitemap/disease-sitemap-" + s + ".xml"), ConfigHelper.getAppStart()));
		}

		List<String> alleleKeys = manager.getAlleleKeys();
		Log.info("Allele Keys: " + alleleKeys.size());
		for (String s : alleleKeys) {
			list.add(new SiteMap(buildUrl("api/sitemap/allele-sitemap-" + s + ".xml"), ConfigHelper.getAppStart()));
		}

		List<String> variantKeys = manager.getVariantKeys();
		Log.info("Variant Keys: " + variantKeys.size());
		for (String s : variantKeys) {
			list.add(new SiteMap(buildUrl("api/sitemap/variant-sitemap-" + s + ".xml"), ConfigHelper.getAppStart()));
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

		if (category.equals("gene")) {
			List<XMLURL> list = manager.getGenes(page.toString());
			XMLURLSet set = new XMLURLSet();
			set.setUrl(list);
			for (XMLURL url : list) {
				url.setLoc(buildUrl(url.getLoc()));
			}
			return set;
		}

		if (category.equals("disease")) {
			List<XMLURL> list = manager.getDiseases(page.toString());
			XMLURLSet set = new XMLURLSet();
			set.setUrl(list);
			for (XMLURL url : list) {
				url.setLoc(buildUrl(url.getLoc()));
			}
			return set;
		}

		if (category.equals("allele")) {
			List<XMLURL> list = manager.getAlleles(page.toString());
			XMLURLSet set = new XMLURLSet();
			set.setUrl(list);
			for (XMLURL url : list) {
				url.setLoc(buildUrl(url.getLoc()));
			}
			return set;
		}

		if (category.equals("variant")) {
			List<XMLURL> list = manager.getAlleles(page.toString());
			XMLURLSet set = new XMLURLSet();
			set.setUrl(list);
			for (XMLURL url : list) {
				url.setLoc(buildUrl(url.getLoc()));
			}
			return set;
		}

		return null;
	}

	private String buildUrl(String inUrl) {
		final StringBuilder url = new StringBuilder();
		url.append("https://www.alliancegenome.org");

		if (inUrl != null) {
			url.append("/");
			url.append(inUrl);
		}
		return url.toString();
	}

	@Override
	public Response getURL(String id) {
		String url = manager.getURL(id);
		if(url == null) {
			url = "https://www.alliancegenome.org/" + id;
		}
		
		try {
			URI uri = new URI(url);
			Response resp = Response.temporaryRedirect(uri).status(Status.PERMANENT_REDIRECT).build();
			return resp;
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		return null;
	}

}
