package org.alliancegenome.indexer.schema.mappings;

import org.alliancegenome.indexer.schema.Mappings;

import java.io.IOException;

public class DiseaseMappings extends Mappings {

	public DiseaseMappings(Boolean pretty) {
		super(pretty);
	}

	public void buildMappings() {
		try {

			builder.startObject();


				builder.startObject("properties");
	
					buildProperty("primaryId", "keyword");
					buildProperty("external_ids", "text", "symbols");
	
				builder.endObject();

			builder.endObject();


		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
