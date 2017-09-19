package org.alliancegenome.indexer.schema;

import java.io.IOException;

public abstract class Settings extends Builder {

	public Settings(Boolean pretty) {
		super(pretty);
	}

	public abstract void buildSettings() throws IOException;
}
