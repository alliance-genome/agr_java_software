package org.alliancegenome.indexer.schema.mappings;

import java.io.IOException;

import org.alliancegenome.indexer.schema.Mappings;

public class GoMappings extends Mappings {
	
	public GoMappings(Boolean pretty) {
		super(pretty);
	}

	public void buildMappings() {
		try {
			builder.startObject();
				builder.startObject("properties");
			
				buildProperty("description", "text");
				buildGenericField("go_genes", "text", "symbols", false, false, true);
				buildGenericField("go_species", "text", null, false, false, true);
				buildGenericField("go_type", "text", null, false, false, true);
				
				buildProperty("href", "text", "symbols");
				buildProperty("id", "text", "symbols");
				
				buildGenericField("name", "text", null, true, true, true);
				buildGenericField("name_key", "text", "symbols", false, true, false);

				builder.endObject();
			builder.endObject();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
