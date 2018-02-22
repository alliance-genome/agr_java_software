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

            builder.endObject();

            builder.endObject();


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
