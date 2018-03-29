package org.alliancegenome.es.schema.mapping;

import java.io.IOException;

import org.alliancegenome.es.schema.Mapping;

public class FeatureMapping extends Mapping {

	public FeatureMapping(Boolean pretty) {
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
