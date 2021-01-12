package org.alliancegenome.api.model.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import lombok.*;

@Getter
@Setter
@Schema(name="SiteMapIndex", description="POJO that represents the SiteMapIndex")
@XmlRootElement(name = "sitemapindex")
public class SiteMapIndex {

    private List<SiteMap> sitemap;
    
}
