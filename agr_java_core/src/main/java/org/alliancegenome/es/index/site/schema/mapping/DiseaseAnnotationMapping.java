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

            new FieldBuilder(builder,"publications.pubModId","text").symbol().autocomplete().keyword().standardText().build();
            new FieldBuilder(builder,"publications.pubMedId","text").symbol().autocomplete().keyword().standardText().build();
            new FieldBuilder(builder,"source.species.name","text").symbol().autocomplete().keyword().standardText().sort().build();

            buildNestedDocument("featureDocument");
            buildNestedDocument("geneDocument");

            builder.endObject();
            builder.endObject();


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

}
