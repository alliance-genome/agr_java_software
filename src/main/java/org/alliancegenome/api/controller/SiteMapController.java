package org.alliancegenome.api.controller;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.UriInfo;

import org.alliancegenome.api.model.SearchResult;
import org.alliancegenome.api.model.SiteMap;
import org.alliancegenome.api.model.SiteMapIndex;
import org.alliancegenome.api.rest.interfaces.SiteMapRESTInterface;
import org.alliancegenome.api.service.SearchService;
import org.jboss.logging.Logger;

@RequestScoped
public class SiteMapController implements SiteMapRESTInterface {
	
	@Inject
	private SearchService searchService;
	private Logger log = Logger.getLogger(getClass());
	
	@Override
	public SiteMapIndex getGeneSiteMap(UriInfo uriInfo) {
		
		List<SiteMap> list = new ArrayList<SiteMap>();

		int chunk = 1000;
		int c = 0, rc = 1;
		do {
			SearchResult sr = searchService.query(null, "go", chunk, c * chunk, null, uriInfo);
			rc = sr.results.size();
			for(Map<String, Object> map: sr.results) {
				list.add(new SiteMap(buildUrl(uriInfo, (String)map.get("category"), (String)map.get("id")), new Date()));
			}
			c++;
		} while(rc > 0);
		
		SiteMapIndex index = new SiteMapIndex();
		index.setSitemap(list);
		return index;
	}
	
	private String buildUrl(UriInfo uriInfo, String category, String id) {
	    final URI uri = uriInfo.getAbsolutePath();
	    final StringBuilder url = new StringBuilder();
	    url.append(uri.getScheme());
	    url.append("://");
	    url.append(uri.getHost());

	    final int port = uri.getPort();
	    if (port != -1) {
	        url.append(":");
	        url.append(uri.getPort());
	    }
	    url.append("/");
	    url.append(category);
	    url.append("/");
	    url.append(id);
	    return url.toString();
	}

}
