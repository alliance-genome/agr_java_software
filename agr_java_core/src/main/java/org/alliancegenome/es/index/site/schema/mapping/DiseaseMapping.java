package org.alliancegenome.es.index.site.schema.mapping;

import java.io.IOException;

import org.alliancegenome.es.index.site.schema.Mapping;

public class DiseaseMapping extends Mapping {

    public DiseaseMapping(Boolean pretty) {
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
