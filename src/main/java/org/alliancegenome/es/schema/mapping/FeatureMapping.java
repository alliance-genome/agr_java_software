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

			buildNestedDocument("geneDocument");
			buildNestedDocument("diseaseDocuments");

			builder.endObject();

			builder.endObject();


		} catch (IOException e) {
			e.printStackTrace();
		}

	}

}
