package org.alliancegenome.indexer.schema.mappings;

import org.alliancegenome.indexer.schema.Mappings;

import java.io.IOException;

public class DiseaseMappings extends Mappings {

    public DiseaseMappings(Boolean pretty) {
        super(pretty);
    }

    public void buildMappings(boolean enclose) {
        try {

            if (enclose) builder.startObject();
            else {
                builder.startObject("mappings");
                builder.startObject("searchable_item");
            }

            builder.startObject("properties");

            //buildProperty("primaryId", "keyword");

            builder.endObject();

            if (enclose) builder.endObject();
            else {
                builder.endObject();
                builder.endObject();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
