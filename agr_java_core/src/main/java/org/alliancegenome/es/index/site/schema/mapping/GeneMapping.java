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
                    new FieldBuilder(builder,"biologicalProcess","text").keyword().build();
                    new FieldBuilder(builder,"cellularComponent","text").keyword().build();
                    new FieldBuilder(builder,"molecularFunction","text").keyword().build();
                    new FieldBuilder(builder,"biologicalProcessWithParents","text").keyword().build();
                    new FieldBuilder(builder,"cellularComponentWithParents","text").keyword().build();
                    new FieldBuilder(builder,"molecularFunctionWithParents","text").keyword().build();
                    new FieldBuilder(builder,"biologicalProcessAgrSlim","text").keyword().build();
                    new FieldBuilder(builder,"cellularComponentAgrSlim","text").keyword().build();
                    new FieldBuilder(builder,"molecularFunctionAgrSlim","text").keyword().build();
                    new FieldBuilder(builder,"strictOrthologySymbols","text").keyword().autocomplete().build();
                    new FieldBuilder(builder,"whereExpressed","text").keyword().build();
                    new FieldBuilder(builder,"anatomicalExpression","text").keyword().build();
                    new FieldBuilder(builder,"cellularComponentExpression","text").keyword().build();
                    new FieldBuilder(builder,"cellularComponentExpressionWithParents","text").keyword().build();
                    new FieldBuilder(builder,"cellularComponentExpressionAgrSlim","text").keyword().build();

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
