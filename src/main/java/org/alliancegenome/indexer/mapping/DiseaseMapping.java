package org.alliancegenome.indexer.mapping;

import java.io.IOException;

public class DiseaseMapping extends Mapping {
	
	public DiseaseMapping(Boolean pretty) {
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
