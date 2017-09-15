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

            buildGenericField("geneDocument.symbol", "string", null, false, false, true);
            buildGenericField("disease_species.orderID", "long", null, false, false, true);
            buildGenericField("diseaseID", "string", null, false, false, true);
            buildGenericField("diseaseName", "string", null, false, false, true);
            buildGenericField("parentDiseaseIDs", "string", null, false, false, true);

            builder.endObject();

            builder.endObject();


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
