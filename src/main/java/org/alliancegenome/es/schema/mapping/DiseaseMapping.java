package org.alliancegenome.es.schema.mapping;

import java.io.IOException;

import org.alliancegenome.es.schema.Mapping;

public class DiseaseMapping extends Mapping {

    public DiseaseMapping(Boolean pretty) {
        super(pretty);
    }

    public void buildMappings() {
        try {

            builder.startObject();

            builder.startObject("properties");

            buildSharedSearchableDocumentMappings();

            buildGenericField("disease_species.orderID", "long", null, false, false, true, false, false);
            buildGenericField("diseaseID", "keyword", null, false, false, true, false, false);
            buildGenericField("diseaseName", "text", null, false, false, true, false, false);
            buildGenericField("parentDiseaseIDs", "keyword", null, false, false, true, false, false);

            builder.startObject("annotations");
            builder.startObject("properties");
            buildNestedDocument("featureDocument");
            buildNestedDocument("geneDocument");
            builder.endObject();
            builder.endObject();

            builder.endObject();

            builder.endObject();

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
