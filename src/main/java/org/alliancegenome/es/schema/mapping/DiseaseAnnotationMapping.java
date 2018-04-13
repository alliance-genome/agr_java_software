package org.alliancegenome.es.schema.mapping;

import org.alliancegenome.es.schema.Mapping;

import java.io.IOException;

public class DiseaseAnnotationMapping extends Mapping {

    public DiseaseAnnotationMapping(Boolean pretty) {
        super(pretty);
    }

    public void buildMappings() {
        try {

            builder.startObject();
            builder.startObject("properties");

            buildSharedSearchableDocumentMappings();

            buildNestedDocument("featureDocument");
            buildNestedDocument("geneDocument");

            builder.endObject();
            builder.endObject();


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
