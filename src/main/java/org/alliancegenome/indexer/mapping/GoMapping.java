package org.alliancegenome.indexer.mapping;

import java.io.IOException;

public class GoMapping extends Mapping {
	
	public GoMapping(Boolean pretty) {
		super(pretty);
	}

	public String buildMapping() {
		try {
			builder.startObject();
			builder.endObject();
			return builder.string();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}
	
}
