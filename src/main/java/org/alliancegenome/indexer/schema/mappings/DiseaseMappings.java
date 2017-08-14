package org.alliancegenome.indexer.schema.mappings;

import java.io.IOException;

import org.alliancegenome.indexer.schema.Mappings;

public class DiseaseMappings extends Mappings {
	
	public DiseaseMappings(Boolean pretty) {
		super(pretty);
	}

	public void buildMappings(boolean enclose) {
		try {
			builder.startObject();
			builder.endObject();
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
}
