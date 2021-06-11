package org.alliancegenome.es.index.site.schema;

import java.io.IOException;

public class VariantMapping extends Mapping {

    public VariantMapping(Boolean pretty) {
        super(pretty);
    }

    @Override
    public void buildMapping() {
        try {
            builder.startObject().startObject("properties");
            new FieldBuilder(builder, "category", "keyword").symbol().autocomplete().keyword().build();
            new FieldBuilder(builder, "name", "keyword").build();
            new FieldBuilder(builder, "name_key", "keyword").build();
            new FieldBuilder(builder, "alterationType", "text").keyword().build();
            new FieldBuilder(builder, "genes", "text").keyword().build();
            
            builder.startObject("allele");
            builder.startObject("properties");
            builder.startObject("variants");
            builder.startObject("properties");
            builder.startObject("transcriptLevelConsequence");
            builder.startObject("properties");
            new FieldBuilder(builder, "cdsStartPosition", "long").notIndexed().build();
            new FieldBuilder(builder, "cdnaStartPosition", "long").notIndexed().build();
            new FieldBuilder(builder, "proteinStartPosition", "long").notIndexed().build();
            new FieldBuilder(builder, "siftScore", "double").notIndexed().build();
            new FieldBuilder(builder, "polyphenScore", "double").notIndexed().build();
            builder.endObject();
            builder.endObject();
            builder.endObject();
            builder.endObject();
            builder.endObject();
            builder.endObject();
            builder.endObject().endObject();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
