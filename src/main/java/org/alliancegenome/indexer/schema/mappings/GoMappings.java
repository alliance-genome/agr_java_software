package org.alliancegenome.indexer.schema.mappings;

import java.io.IOException;

import org.alliancegenome.indexer.schema.Mappings;

public class GoMappings extends Mappings {
	
	public GoMappings(Boolean pretty) {
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
