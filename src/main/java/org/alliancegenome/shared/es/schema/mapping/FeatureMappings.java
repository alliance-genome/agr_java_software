package org.alliancegenome.shared.es.schema.mapping;

import java.io.IOException;

import org.alliancegenome.shared.es.schema.Mappings;

public class FeatureMappings extends Mappings {

	public FeatureMappings(Boolean pretty) {
		super(pretty);
	}

	public void buildMappings() {
		try {

			builder.startObject();


			builder.startObject("properties");

			buildSharedSearchableDocumentMappings();

			buildGenericField("symbol", "text", "symbols", false, false, true, false);
			builder.endObject();

			builder.endObject();


		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
