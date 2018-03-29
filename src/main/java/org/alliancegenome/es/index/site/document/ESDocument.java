package org.alliancegenome.es.index.site.document;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class ESDocument {

	@JsonIgnore
	public abstract String getDocumentId();

	@JsonIgnore
	public abstract String getType();
}
