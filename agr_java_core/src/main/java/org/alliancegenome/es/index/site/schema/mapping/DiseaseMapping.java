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

            builder.startObject("disease_species");
                builder.startObject("properties");
                    new FieldBuilder(builder, "orderID", "long").keyword().build();
                builder.endObject();
            builder.endObject();

            new FieldBuilder(builder, "diseaseID", "keyword").keyword().build(); //odd that a keyword field has a keyword subfield...necessary?
            new FieldBuilder(builder, "diseaseName", "text").keyword().build();
            new FieldBuilder(builder, "parentDiseaseIDs", "keyword").keyword().build(); //odd for the same reason

            builder.startObject("annotations");
            builder.startObject("properties");
            buildNestedDocument("alleleDocument");
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
