package org.alliancegenome.api.controller;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.alliancegenome.api.model.xml.SiteMap;
import org.alliancegenome.api.model.xml.SiteMapIndex;
import org.alliancegenome.api.model.xml.XMLURL;
import org.alliancegenome.api.model.xml.XMLURLSet;
import org.alliancegenome.api.rest.interfaces.SiteMapRESTInterface;
import org.alliancegenome.api.service.SiteMapService;
import org.alliancegenome.core.config.ConfigHelper;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.search.SearchHit;

import io.quarkus.logging.Log;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

@RequestScoped
public class SiteMapController implements SiteMapRESTInterface {

	@Inject
	SiteMapService siteMapService;

	@Override
	public SiteMapIndex getSiteMap() {
		Log.info("Serving SiteMap.xml");
		SearchResponse resp = siteMapService.getFullSiteMap();
		List<SiteMap> list = new ArrayList<SiteMap>();
		for (SearchHit searchHit : resp.getHits().getHits()) {
			String siteMapId = searchHit.getSourceAsMap().get("siteMapId").toString();
			list.add(new SiteMap(buildUrl("api/sitemap/" + siteMapId + ".xml"), ConfigHelper.getAppStart()));
		}
		SiteMapIndex index = new SiteMapIndex();
		index.setSitemap(list);
		return index;
	}

	@Override
	public XMLURLSet getSiteMap(String siteMapId) {
		Log.info("Serving " + siteMapId + ".xml");
		SearchResponse resp = siteMapService.getSiteMap(siteMapId);
		List<XMLURL> urls = new ArrayList<XMLURL>();

		for (SearchHit searchHit : resp.getHits().getHits()) {
			String siteMapType = (String) searchHit.getSourceAsMap().get("siteMapType");
			List<String> siteMapIds = (ArrayList<String>) searchHit.getSourceAsMap().get("siteMapIds");
			for (String localSiteMapId : siteMapIds) {
				urls.add(new XMLURL(siteMapType + "/" + localSiteMapId, ConfigHelper.getAppStart(), "monthly", "0.6"));
			}
		}

		XMLURLSet set = new XMLURLSet();
		set.setUrl(urls);
		for (XMLURL url : urls) {
			url.setLoc(buildUrl(url.getLoc()));
		}

		return set;
	}

	private String buildUrl(String inUrl) {
		StringBuilder url = new StringBuilder();
		url.append("https://www.alliancegenome.org");

		if (inUrl != null) {
			url.append("/");
			url.append(inUrl);
		}
		return url.toString();
	}

	@Override
	public Response getAccessionURL(String id) {

		Log.info("Id Lookup: " + id);

		Map<String, String> map = Map.of("gene", "primaryKey", "allele", "primaryKey", "variant", "primaryKey", "disease", "primaryKey");

		for (Entry<String, String> entry : map.entrySet()) {
			SearchResponse response = siteMapService.getAccession(entry.getKey(), entry.getValue(), id);

			if (response == null || response.getHits() == null || response.getHits().getHits() == null || response.getHits().getHits().length == 0) {
				continue;
			} else {
				System.out.println(entry + " " + id);

				String url = null;
				if (url == null) {
					url = "https://www.alliancegenome.org/" + entry.getKey() + "/" + id;
				}

				try {
					URI uri = new URI(url);
					Response resp = Response.temporaryRedirect(uri).status(Status.PERMANENT_REDIRECT).build();
					return resp;
				} catch (URISyntaxException e) {
					e.printStackTrace();
				}
			}
		}

		try {
			URI uri = new URI("https://www.alliancegenome.org/" + id);
			Response resp = Response.temporaryRedirect(uri).status(Status.PERMANENT_REDIRECT).build();
			return resp;
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		return null;
	}

}
