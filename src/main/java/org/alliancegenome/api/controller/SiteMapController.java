package org.alliancegenome.api.controller;

import java.net.URI;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.UriInfo;

import org.alliancegenome.api.config.ConfigHelper;
import org.alliancegenome.api.model.SearchResult;
import org.alliancegenome.api.model.xml.SiteMap;
import org.alliancegenome.api.model.xml.SiteMapIndex;
import org.alliancegenome.api.model.xml.XMLURL;
import org.alliancegenome.api.model.xml.XMLURLSet;
import org.alliancegenome.api.rest.interfaces.SiteMapRESTInterface;
import org.alliancegenome.api.service.GeneService;
import org.alliancegenome.api.service.SearchService;
import org.elasticsearch.search.SearchHit;
import org.jboss.logging.Logger;

@RequestScoped
public class SiteMapController implements SiteMapRESTInterface {

    @Inject
    private SearchService searchService;

    @Inject
    private ConfigHelper config;

    private Logger log = Logger.getLogger(getClass());


    @Override
    public SiteMapIndex getSiteMap(UriInfo uriInfo) {
        SiteMapIndex index = new SiteMapIndex();
        List<SiteMap> list = new ArrayList<SiteMap>();
        list.add(new SiteMap(buildUrl(uriInfo, "api/sitemap", "gene.xml"), config.getAppStart()));
        list.add(new SiteMap(buildUrl(uriInfo, "api/sitemap", "disease.xml"), config.getAppStart()));
        //list.add(new SiteMap(buildUrl(uriInfo, "api/sitemap", "go.xml"), new Date()));
        index.setSitemap(list);
        return index;
    }

    @Override
    public XMLURLSet getCategorySiteMap(String category, UriInfo uriInfo) {
        return buildSiteMapByCategory(category, uriInfo);
    }

    private XMLURLSet buildSiteMapByCategory(String category, UriInfo uriInfo) {
        List<XMLURL> urls = new ArrayList<XMLURL>();
        log.info("Site Map Request: " + buildUrl(uriInfo, "api/sitemap", category));
        
        List<String> response_fields = new ArrayList<String>() { { add("category"); add("id"); add("dateProduced"); } };
        List<SearchHit> hits = searchService.getAllByCategory(category, response_fields);
        for(SearchHit hit: hits) {
            urls.add(new XMLURL(buildUrl(uriInfo, (String)hit.getSource().get("category"), (String)hit.getSource().get("id")), (Date)hit.getSource().get("dateProduced"), "monthly", "0.6"));
        }

        XMLURLSet set = new XMLURLSet();
        set.setUrl(urls);
        log.info(urls.size() + " URL's returned");
        return set;
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
        if(category != null) {
            url.append("/");
            url.append(category);
        }
        if(id != null) {
            url.append("/");
            url.append(id);
        }
        return url.toString();
    }

}
