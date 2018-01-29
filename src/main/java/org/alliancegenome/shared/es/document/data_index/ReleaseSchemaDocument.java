package org.alliancegenome.shared.es.document.data_index;

import org.alliancegenome.shared.es.document.site_index.ESDocument;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReleaseSchemaDocument extends ESDocument {

	private String type = "release_schema";

	private String id;
	private String relaeseVersion;
	private String schemaVersion;

	@Override
	public String getDocumentId() {
		return id;
	}

}
