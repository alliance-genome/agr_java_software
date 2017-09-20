package org.alliancegenome.api.model;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter @AllArgsConstructor @NoArgsConstructor
@XmlRootElement(name="sitemap")
public class SiteMap {

	private String loc;
	private Date lastmod;
	
}
