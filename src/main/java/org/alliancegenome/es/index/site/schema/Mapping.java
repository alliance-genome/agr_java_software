package org.alliancegenome.es.index.site.schema;

import java.io.IOException;

import org.alliancegenome.es.index.site.schema.mapping.*;

public abstract class Mapping extends Builder {
    
    public Mapping(Boolean pretty) {
        super(pretty);
    }

    public abstract void buildMappings();

    protected void buildGenericField(String name, String type, String analyzer, boolean symbol, boolean autocomplete, boolean keyword, boolean synonym, boolean sort) throws IOException {
        builder.startObject(name);
        if(type != null) builder.field("type", type);
        if(analyzer != null) builder.field("analyzer", analyzer);
        if(symbol || autocomplete || keyword || synonym || sort) {
            builder.startObject("fields");
            if(keyword) { buildProperty("keyword", "keyword"); }
            if(symbol) buildGenericField("symbol", "text", "symbols", false, false, false, false, false);
            if(autocomplete) buildProperty("autocomplete", "text", "autocomplete", "autocomplete_search", null);
            if(synonym) buildProperty("synonyms", "text", "generic_synonym", "autocomplete_search", null);
            if(sort) buildProperty("sort", "keyword", null, null, "lowercase");
            builder.endObject();
        }
        builder.endObject();
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

    //Mappings that must be shared / equivalent across searchable documents
    protected void buildSharedSearchableDocumentMappings() throws IOException {

        buildProperty("primaryId", "keyword");
        buildGenericField("category", "keyword", null, true, true, true, false, false);
        buildGenericField("name", "text", null, true, true, true, false, false);
        buildGenericField("name_key", "text", "symbols", false, true, false, false, false);
        buildGenericField("synonyms", "text", "symbols", false, true, true, false, false);
        buildProperty("external_ids", "text", "symbols");
        buildProperty("href", "text", "symbols");
        buildProperty("id", "text", "symbols");
        buildProperty("description", "text");
        buildGenericField("symbol", "text", "symbols", false, false, true, false, true);
        buildGenericField("species", "text", null, false, false, true, true, false);
    }

    protected void buildNestedDocument(String name) throws IOException {
        builder.startObject(name);
        builder.startObject("properties");
        //likely more fields than most will need, but the schema will be there for as many as are necessary
        buildSharedSearchableDocumentMappings();
        builder.endObject();
        builder.endObject();
    }

    public enum MappingClass {
        Disease("disease", DiseaseMapping.class),
        DiseaseAnnotation("diseaseAnnotation", DiseaseAnnotationMapping.class),
        Feature("feature", FeatureMapping.class),
        Gene("gene", GeneMapping.class),
        Go("go", GoMapping.class),
        ;
        
        private String type;
        private Class<?> mappingClass;

        private MappingClass(String type, Class<?> mappingClass) {
            this.type = type;
            this.mappingClass = mappingClass;
        }
        
        public String getType() {
            return type;
        }
        public Class<?> getMappingClass() {
            return mappingClass;
        }
    }
    
}
