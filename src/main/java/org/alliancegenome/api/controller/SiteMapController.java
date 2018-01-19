package org.alliancegenome.api.controller;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.UriInfo;

import org.alliancegenome.api.application.SiteMapCacherApplication;
import org.alliancegenome.api.config.ConfigHelper;
import org.alliancegenome.api.model.xml.SiteMap;
import org.alliancegenome.api.model.xml.SiteMapIndex;
import org.alliancegenome.api.model.xml.XMLURL;
import org.alliancegenome.api.model.xml.XMLURLSet;
import org.alliancegenome.api.rest.interfaces.SiteMapRESTInterface;

@RequestScoped
public class SiteMapController extends BaseController implements SiteMapRESTInterface {

    @Inject
    private SiteMapCacherApplication siteMapApp;

    @Inject
    private ConfigHelper config;

    //private final Logger log = Logger.getLogger(getClass());

    @Override
    public SiteMapIndex getSiteMap(UriInfo uriInfo) {
        SiteMapIndex index = new SiteMapIndex();
        List<SiteMap> list = new ArrayList<SiteMap>();
        Set<String> files = siteMapApp.getFiles();
        for(String file: files) {
            list.add(new SiteMap(buildUrl(uriInfo, "api/sitemap/" + file + ".xml"), config.getAppStart()));
        }
        index.setSitemap(list);
        return index;
    }

    @Override
    public XMLURLSet getCategorySiteMap(String category, Integer page, UriInfo uriInfo) {
        return buildSiteMapByCategory(category, page, uriInfo);
    }


    private XMLURLSet buildSiteMapByCategory(String category, Integer page, UriInfo uriInfo) {
        XMLURLSet set = siteMapApp.getHits(category, page);
        for(XMLURL url: set.getUrl()) {
            url.setLoc(buildUrl(uriInfo, url.getLoc()));
        }
        return set;
    }

    private String buildUrl(UriInfo uriInfo, String inUrl) {
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
        if(inUrl != null) {
            url.append("/");
            url.append(inUrl);
        }
        return url.toString();
    }

}
