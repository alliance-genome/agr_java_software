package org.alliancegenome.indexer.schema.mappings;

import java.io.IOException;

import org.alliancegenome.indexer.schema.Mappings;

public class GoMappings extends Mappings {
    
    public GoMappings(Boolean pretty) {
        super(pretty);
    }

    public void buildMappings() {
        try {
            builder.startObject();
                builder.startObject("properties");

                buildSharedSearchableDocumentMappings();

                buildGenericField("go_genes", "text", "symbols", false, false, true, false);
                buildGenericField("go_species", "text", null, false, false, true, false);
                buildGenericField("go_type", "text", null, false, false, true, false);

                builder.endObject();
            builder.endObject();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
