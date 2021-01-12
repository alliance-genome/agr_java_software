package org.alliancegenome.api.model.xml;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import lombok.*;

@Getter
@Setter @AllArgsConstructor @NoArgsConstructor
@Schema(name="SiteMap", description="POJO that represents the SiteMap")
@XmlRootElement(name = "sitemap")
public class SiteMap {

    private String loc;
    private Date lastmod;
    
}
