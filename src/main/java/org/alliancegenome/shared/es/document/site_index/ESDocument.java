package org.alliancegenome.shared.es.document.site_index;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class ESDocument {

	@JsonIgnore
	public abstract String getDocumentId();

	@JsonIgnore
	public abstract String getType();
}
