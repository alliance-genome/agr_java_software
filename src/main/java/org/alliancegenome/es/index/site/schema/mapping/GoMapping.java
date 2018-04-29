package org.alliancegenome.es.index.site.schema.mapping;

import java.io.IOException;

import org.alliancegenome.es.index.site.schema.Mapping;

public class GoMapping extends Mapping {
    
    public GoMapping(Boolean pretty) {
        super(pretty);
    }

    public void buildMappings() {
        try {
            builder.startObject();
                builder.startObject("properties");

                buildSharedSearchableDocumentMappings();

                new FieldBuilder(builder, "go_genes", "text").analyzer("symbols").keyword().build();
                new FieldBuilder(builder, "go_species", "text").keyword().build();
                new FieldBuilder(builder, "go_type", "text").keyword().build();

                builder.endObject();
            builder.endObject();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
}
