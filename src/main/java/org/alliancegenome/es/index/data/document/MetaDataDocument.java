package org.alliancegenome.es.index.data.document;

import org.alliancegenome.es.index.site.document.ESDocument;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter @Setter @ToString
public class MetaDataDocument extends ESDocument {

	private String id;
	private String type = "meta_data";
	private String debug;
	private String esHost;
	private String esIndex;
	private String esPort;

	@Override
	public String getDocumentId() {
		return id;
	}

}
