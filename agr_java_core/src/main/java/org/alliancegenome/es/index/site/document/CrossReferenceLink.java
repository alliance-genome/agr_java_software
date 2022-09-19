package org.alliancegenome.es.index.site.document;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrossReferenceLink {
	String displayName;
	String name;
	String url;

	public CrossReferenceLink(String url, String name, String displayName) {
		this.url = url;
		this.name = name;
		this.displayName = displayName;
	}
}
