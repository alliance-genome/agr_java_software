package org.alliancegenome.es.index.site.schema;

import java.io.IOException;

import org.elasticsearch.common.xcontent.XContentBuilder;

public class Mapping extends Builder {
    
    public Mapping(Boolean pretty) {
        super(pretty);
    }

    public void buildMapping() {
        try {
            builder.startObject();
                builder.startObject("properties");
                    buildSharedSearchableDocumentMappings();
                builder.endObject();
            builder.endObject();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected void buildSharedSearchableDocumentMappings() throws IOException {
        new FieldBuilder(builder, "age", "text").keyword().build();
        new FieldBuilder(builder, "alleles", "text").keyword().autocomplete().build();
        new FieldBuilder(builder, "anatomicalExpression", "text").keyword().build();
        new FieldBuilder(builder, "associatedSpecies", "text").keyword().synonym().sort().build();
        new FieldBuilder(builder, "associationType", "text").symbol().autocomplete().keyword().standardText().build();
        new FieldBuilder(builder, "biologicalProcess", "text").keyword().build();
        new FieldBuilder(builder, "biologicalProcessAgrSlim", "text").keyword().build();
        new FieldBuilder(builder, "biologicalProcessWithParents", "text").keyword().build();
        new FieldBuilder(builder, "biotype0","text").keyword().build();
        new FieldBuilder(builder, "biotype1","text").keyword().build();
        new FieldBuilder(builder, "biotype2","text").keyword().build();
        new FieldBuilder(builder, "biotypes", "text").keyword().letterText().build();
        new FieldBuilder(builder, "branch", "text").keyword().build();
        new FieldBuilder(builder, "category", "keyword").symbol().autocomplete().keyword().build();
        new FieldBuilder(builder, "cellularComponent", "text").keyword().build();
        new FieldBuilder(builder, "cellularComponentAgrSlim", "text").keyword().build();
        new FieldBuilder(builder, "cellularComponentWithParents", "text").keyword().build();
        new FieldBuilder(builder, "cellularComponentExpression", "text").keyword().build();
        new FieldBuilder(builder, "cellularComponentExpressionWithParents", "text").keyword().build();
        new FieldBuilder(builder, "cellularComponentExpressionAgrSlim", "text").keyword().build();
        new FieldBuilder(builder, "chromosomes", "text").keyword().build();
        new FieldBuilder(builder, "constructs", "text").keyword().classicText().build();
        new FieldBuilder(builder, "constructExpressedComponent", "text").keyword().build();
        new FieldBuilder(builder, "constructKnockdownComponent", "text").keyword().build();
        new FieldBuilder(builder, "constructRegulatoryRegion", "text").keyword().build();
        new FieldBuilder(builder, "crossReferences", "text").keyword().classicText().build();
        new FieldBuilder(builder, "definition", "text").standardText().build();
        new FieldBuilder(builder, "description", "text").build();
        new FieldBuilder(builder, "diseases", "text").keyword().build();
        new FieldBuilder(builder, "diseasesAgrSlim", "text").keyword().build();
        new FieldBuilder(builder, "diseasesWithParents", "text").keyword().build();
        new FieldBuilder(builder, "expressionStages", "text").keyword().standardText().build();
        new FieldBuilder(builder, "external_ids", "text").analyzer("symbols");
        new FieldBuilder(builder, "genes", "text").keyword().autocomplete().keywordAutocomplete().build();
        new FieldBuilder(builder, "geneLiteratureUrl", "keyword").build();
        new FieldBuilder(builder, "geneSynopsis", "text").build();
        new FieldBuilder(builder, "geneSynopsisUrl", "keyword").build();
        new FieldBuilder(builder, "href", "keyword");
        new FieldBuilder(builder, "id", "keyword");
        new FieldBuilder(builder, "models", "text").keyword().autocomplete().build();
        new FieldBuilder(builder, "molecularConsequence", "text").keyword().build();
        new FieldBuilder(builder, "molecularFunction", "text").keyword().build();
        new FieldBuilder(builder, "molecularFunctionAgrSlim", "text").keyword().build();
        new FieldBuilder(builder, "molecularFunctionWithParents", "text").keyword().build();
        new FieldBuilder(builder, "name", "text")
                .symbol()
                .autocomplete()
                .keyword()
                .keywordAutocomplete()
                .htmlSmoosh()
                .standardBigrams()
                .build();
        new FieldBuilder(builder, "nameText", "text").keyword().standardText().build();
        new FieldBuilder(builder, "name_key", "text").analyzer("symbols")
                .autocomplete()
                .keyword()
                .keywordAutocomplete()
                .htmlSmoosh()
                .standardBigrams()
                .build();
        new FieldBuilder(builder, "phenotypeStatements", "text")
                .keyword()
                .build();
        new FieldBuilder(builder, "popularity", "double").build();
        new FieldBuilder(builder, "primaryKey", "keyword").build();
        new FieldBuilder(builder, "symbol", "text").analyzer("symbols")
                .autocomplete()
                .htmlSmoosh()
                .keyword()
                .keywordAutocomplete()
                .sort()
                .build();
        new FieldBuilder(builder, "searchSymbol", "text").analyzer("symbols")
                .autocomplete()
                .keyword()
                .keywordAutocomplete()
                .sort()
                .build();
        new FieldBuilder(builder, "sex", "text").keyword().build();
        new FieldBuilder(builder, "secondaryIds", "keyword").build();
        new FieldBuilder(builder, "soTermName", "text").keyword().letterText().build();
        new FieldBuilder(builder, "soTermId", "keyword").build();
        new FieldBuilder(builder, "species", "text").keyword().synonym().sort().build();
        new FieldBuilder(builder, "strictOrthologySymbols", "text").keyword().autocomplete().build();
        new FieldBuilder(builder, "summary", "text").build();
        new FieldBuilder(builder, "symbolText", "text").keyword().standardText().build();
        new FieldBuilder(builder, "synonyms", "text").analyzer("symbols")
                .autocomplete()
                .keyword()
                .keywordAutocomplete()
                .htmlSmoosh()
                .standardBigrams()
                .build();
        new FieldBuilder(builder, "systematicName", "text").analyzer("symbols").build();
        new FieldBuilder(builder, "taxonId", "keyword").build();
        new FieldBuilder(builder, "variants", "text").keyword().standardText().build();
        new FieldBuilder(builder, "variantType", "text").keyword().build();
        new FieldBuilder(builder, "variantSynonyms", "text").keyword().standardText().build();
        new FieldBuilder(builder, "whereExpressed", "text").keyword().build();

        buildMetaDataField();


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

    protected void buildNestedDocument(String name) throws IOException {
        builder.startObject(name);
        builder.startObject("properties");
        //likely more fields than most will need, but the schema will be there for as many as are necessary
        buildSharedSearchableDocumentMappings();
        builder.endObject();
        builder.endObject();
    }

    public static class FieldBuilder {
        XContentBuilder builder;
        String name;
        String type;
        String analyzer;
        boolean autocomplete;
        boolean classicText;
        boolean htmlSmoosh;
        boolean keyword;
        boolean keywordAutocomplete;
        boolean letterText;
        boolean sort;
        boolean standardBigrams;
        boolean standardText;
        boolean symbol;
        boolean synonym;

        public FieldBuilder(XContentBuilder builder, String name, String type) {
            this.builder = builder;
            this.name = name;
            this.type = type;
        }

        public FieldBuilder analyzer(String analyzer) {
            this.analyzer = analyzer;
            return this;
        }

        public FieldBuilder autocomplete() {
            this.autocomplete = true;
            return this;
        }

        public FieldBuilder classicText() {
            this.classicText = true;
            return this;
        }

        public FieldBuilder htmlSmoosh() {
            this.htmlSmoosh = true;
            return this;
        }

        public FieldBuilder keyword() {
            this.keyword = true;
            return this;
        }

        public FieldBuilder keywordAutocomplete() {
            this.keywordAutocomplete = true;
            return this;
        }

        public FieldBuilder letterText() {
            this.letterText = true;
            return this;
        }

        public FieldBuilder sort() {
            this.sort = true;
            return this;
        }

        public FieldBuilder standardBigrams() {
            this.standardBigrams = true;
            return this;
        }

        public FieldBuilder standardText() {
            this.standardText = true;
            return this;
        }

        public FieldBuilder symbol() {
            this.symbol = true;
            return this;
        }

        public FieldBuilder synonym() {
            this.synonym = true;
            return this;
        }

        protected void buildProperty(String name, String type) throws IOException {
            buildProperty(name, type, null, null, null);
        }

        protected void buildProperty(String name, String type, String analyzer) throws IOException {
            buildProperty(name, type, analyzer, null, null);
        }

        protected void buildProperty(String name, String type, String analyzer, String search_analyzer, String normalizer) throws IOException {
            builder.startObject(name);
            if(type != null) builder.field("type", type);
            if(analyzer != null) builder.field("analyzer", analyzer);
            if(search_analyzer != null) builder.field("search_analyzer", search_analyzer);
            if(normalizer!= null) builder.field("normalizer", normalizer);
            builder.endObject();
        }


        public void build() throws IOException {
            builder.startObject(name);
            if(type != null) builder.field("type", type);
            if(analyzer != null) builder.field("analyzer", analyzer);
            if(symbol || autocomplete || keyword || keywordAutocomplete || synonym || sort || standardText) {
                builder.startObject("fields");
                if(keyword) { buildProperty("keyword", "keyword"); }
                if(keywordAutocomplete) { buildProperty("keywordAutocomplete", "text", "keyword_autocomplete", "keyword_autocomplete_search", null); }
                if(letterText) buildProperty("letterText", "text", "letter_text", "default", null);
                if(symbol) { buildProperty("symbol", "text", "symbols"); }
                if(autocomplete) buildProperty("autocomplete", "text", "autocomplete", "autocomplete_search", null);
                if(classicText) buildProperty("classicText", "text", "classic_text", "default", null);
                if(synonym) buildProperty("synonyms", "text", "generic_synonym", "autocomplete_search", null);
                if(sort) buildProperty("sort", "keyword", null, null, "lowercase");
                if(htmlSmoosh) buildProperty("htmlSmoosh", "text", "html_smoosh");
                if(standardBigrams) buildProperty("standardBigrams", "text", "standard_bigrams");
                if(standardText) buildProperty("standardText", "text", "standard_text", "default", null);
                builder.endObject();
            }
            builder.endObject();
        }
    }

}
