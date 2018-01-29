package org.alliancegenome.shared.es.document.data_index;

import org.alliancegenome.shared.es.document.site_index.ESDocument;

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
