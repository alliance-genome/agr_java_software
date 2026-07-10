package org.alliancegenome.core.document;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class ESDocument {

	@JsonIgnore
	public abstract String getType();
}
