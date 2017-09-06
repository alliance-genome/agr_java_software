package org.alliancegenome.indexer.document;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class ESDocument {

	@JsonIgnore
	public abstract String getDocumentId();
}
