package org.alliancegenome.es.index.site.schema.mapping;

import java.io.IOException;

import org.alliancegenome.es.index.site.schema.Mapping;

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
