package org.alliancegenome.indexer.schema.mappings;

import org.alliancegenome.indexer.schema.Mappings;

import java.io.IOException;

public class FeatureMappings extends Mappings {

    public FeatureMappings(Boolean pretty) {
        super(pretty);
    }

    public void buildMappings() {
        try {

            builder.startObject();


            builder.startObject("properties");

            buildSharedSearchableDocumentMappings();

            buildGenericField("geneDocument.symbol", "text", null, false, false, true, false);
            buildGenericField("symbol", "keyword", null, false, false, true, false);

            builder.endObject();

            builder.endObject();


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
