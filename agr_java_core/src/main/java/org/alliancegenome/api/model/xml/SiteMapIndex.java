package org.alliancegenome.api.model.xml;

import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@XmlRootElement(name = "sitemapindex")
public class SiteMapIndex {

	private List<SiteMap> sitemap;
	
}
