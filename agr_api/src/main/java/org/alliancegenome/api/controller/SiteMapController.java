package org.alliancegenome.api.controller;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.UriInfo;

import org.alliancegenome.api.model.xml.SiteMap;
import org.alliancegenome.api.model.xml.SiteMapIndex;
import org.alliancegenome.api.model.xml.XMLURL;
import org.alliancegenome.api.model.xml.XMLURLSet;
import org.alliancegenome.api.rest.interfaces.SiteMapRESTInterface;
import org.alliancegenome.cache.repository.SiteMapCacheManager;
import org.alliancegenome.core.config.ConfigHelper;

import lombok.extern.log4j.Log4j2;

@Log4j2
@RequestScoped
public class SiteMapController extends BaseController implements SiteMapRESTInterface {

    @Inject
    private SiteMapCacheManager manager;
    //private final Logger log = Logger.getLogger(getClass());

    @Override
    public SiteMapIndex getSiteMap(UriInfo uriInfo) {
        
        List<SiteMap> list = new ArrayList<SiteMap>();
        
        List<String> geneKeys = manager.getGenesKeys();
        log.info("Gene Keys: "  + geneKeys.size());
        for(String s: geneKeys) {
            list.add(new SiteMap(buildUrl(uriInfo, "api/sitemap/gene-sitemap-" + s + ".xml"), ConfigHelper.getAppStart()));
        }
        
        List<String> diseaseKeys = manager.getDiseaseKeys();
        log.info("Disease Keys: "  + diseaseKeys.size());
        for(String s: diseaseKeys) {
            list.add(new SiteMap(buildUrl(uriInfo, "api/sitemap/disease-sitemap-" + s + ".xml"), ConfigHelper.getAppStart()));
        }
        
        SiteMapIndex index = new SiteMapIndex();
        index.setSitemap(list);
        return index;
    }

    @Override
    public XMLURLSet getCategorySiteMap(String category, Integer page, UriInfo uriInfo) {
        return buildSiteMapByCategory(category, page, uriInfo);
    }


    private XMLURLSet buildSiteMapByCategory(String category, Integer page, UriInfo uriInfo) {

        if(category.equals("gene")) {
            List<XMLURL> list = manager.getGenes(page.toString());
            XMLURLSet set = new XMLURLSet();
            set.setUrl(list);
            for(XMLURL url: list) {
                url.setLoc(buildUrl(uriInfo, url.getLoc()));
            }
            return set;
        }
        
        if(category.equals("disease")) {
            List<XMLURL> list = manager.getDiseases(page.toString());
            XMLURLSet set = new XMLURLSet();
            set.setUrl(list);
            for(XMLURL url: list) {
                url.setLoc(buildUrl(uriInfo, url.getLoc()));
            }
            return set;
        }

        return null;
    }

    private String buildUrl(UriInfo uriInfo, String inUrl) {
        final URI uri = uriInfo.getAbsolutePath();
        final StringBuilder url = new StringBuilder();
        url.append("https://");
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
