package org.alliancegenome.indexer.schema.mappings;

import org.alliancegenome.indexer.schema.Mappings;

import java.io.IOException;

public class DiseaseMappings extends Mappings {

    public DiseaseMappings(Boolean pretty) {
        super(pretty);
    }

    public void buildMappings() {
        try {

            builder.startObject();


            builder.startObject("properties");

            buildSharedSearchableDocumentMappings();

            buildGenericField("geneDocument.symbol", "text", null, false, false, true, false);
            buildGenericField("disease_species.orderID", "long", null, false, false, true, false);
            buildGenericField("diseaseID", "keyword", null, false, false, true, false);
            buildGenericField("diseaseName", "text", null, false, false, true, false);
            buildGenericField("parentDiseaseIDs", "keyword", null, false, false, true, false);

            builder.endObject();

            builder.endObject();


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
