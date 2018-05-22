package org.alliancegenome.es.index.site.schema.mapping;

import java.io.IOException;

import org.alliancegenome.es.index.site.schema.Mapping;

public class GeneMapping extends Mapping {
    
    public GeneMapping(Boolean pretty) {
        super(pretty);
    }

    public void buildMappings() {
        try {

            builder.startObject();
                builder.startObject("properties");

                    buildSharedSearchableDocumentMappings();

                    new FieldBuilder(builder,"systematicName","text").analyzer("symbols").build();

                    buildCrossReferencesField();
                    buildMetaDataField();

                    new FieldBuilder(builder,"geneLiteratureUrl","keyword").build();

                    new FieldBuilder(builder,"geneSynopsis","text").build();
                    new FieldBuilder(builder,"geneSynopsisUrl","keyword").build();
                    new FieldBuilder(builder,"gene_biological_process","text").keyword().build();
                    new FieldBuilder(builder,"gene_cellular_component","text").keyword().build();
                    new FieldBuilder(builder,"gene_molecular_function","text").keyword().build();

                    buildGenomeLocationsField();
                    buildNestedDocument("alleles");
                    buildNestedDocument("diseases");

                    new FieldBuilder(builder,"secondaryIds","keyword").build();
                    new FieldBuilder(builder,"soTermId","keyword").build();
                    new FieldBuilder(builder,"taxonId","keyword").build();
                    
                builder.endObject();
            builder.endObject();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    

    private void buildMetaDataField() throws IOException {
        builder.startObject("metaData");
        builder.startObject("properties");
        new FieldBuilder(builder,"dateProduced","date").build();
        new FieldBuilder(builder,"dataProvider","keyword").build();
        new FieldBuilder(builder,"release","keyword").build();
        builder.endObject();
        builder.endObject();
    }

    private void buildGenomeLocationsField() throws IOException {
        builder.startObject("genomeLocations");
        builder.startObject("properties");
        new FieldBuilder(builder,"assembly","keyword").build();
        new FieldBuilder(builder,"startPosition","integer").build();
        new FieldBuilder(builder,"endPosition","integer").build();
        new FieldBuilder(builder,"chromosome","keyword").build();
        new FieldBuilder(builder,"strand","keyword").build();
        builder.endObject();
        builder.endObject();
    }

    private void buildCrossReferencesField() throws IOException {
        builder.startObject("crossReferences");
        builder.startObject("properties");
        new FieldBuilder(builder,"dataProvider","keyword").build();
        new FieldBuilder(builder,"id","keyword").build();
        builder.endObject();
        builder.endObject();
    }
}
