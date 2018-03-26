package org.alliancegenome.es.index.data.document;

import org.alliancegenome.es.index.site.document.ESDocument;

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
