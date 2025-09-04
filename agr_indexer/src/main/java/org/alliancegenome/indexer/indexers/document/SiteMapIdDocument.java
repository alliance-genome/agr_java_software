package org.alliancegenome.indexer.indexers.document;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.alliancegenome.curation_api.model.document.es.ESDocument;

import lombok.Data;

@Data
public class SiteMapIdDocument extends ESDocument {
	
	{
		category = "sitemapid";
	}
	
	private Date lastMod;
	private String siteMapId;
	private String siteMapType;
	private List<String> siteMapIds = new ArrayList<>();

}
