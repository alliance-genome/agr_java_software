package org.alliancegenome.es.schema.mapping;

import java.io.IOException;

import org.alliancegenome.es.schema.Mapping;

public class GoMapping extends Mapping {
	
	public GoMapping(Boolean pretty) {
		super(pretty);
	}

	public void buildMappings() {
		try {
			builder.startObject();
				builder.startObject("properties");

				buildSharedSearchableDocumentMappings();

				buildGenericField("go_genes", "text", "symbols", false, false, true, false);
				buildGenericField("go_species", "text", null, false, false, true, false);
				buildGenericField("go_type", "text", null, false, false, true, false);

				builder.endObject();
			builder.endObject();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
