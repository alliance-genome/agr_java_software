package org.alliancegenome.indexer.schema;

import java.io.IOException;

public abstract class Mappings extends Builder {
	
	public Mappings(Boolean pretty) {
		super(pretty);
	}

	public abstract void buildMappings();
	

	protected void buildGenericField(String name, String type, String analyzer, boolean symbol, boolean autocomplete, boolean keyword) throws IOException {
		builder.startObject(name);
		if(type != null) builder.field("type", type);
		if(analyzer != null) builder.field("analyzer", analyzer);
		if(symbol || autocomplete || keyword) {
			builder.startObject("fields");
			if(keyword) {
				//temporarily index into both raw & keyword, for backwards compatibility with api repo
				buildProperty("keyword", "keyword");
				buildProperty("raw", "keyword");
			}
			if(symbol) buildGenericField("symbol", "text", "symbols", false, false, false);
			if(autocomplete) buildProperty("autocomplete", "text", "autocomplete", "autocomplete_search");
			builder.endObject();
		}
		builder.endObject();
	}
	
	protected void buildProperty(String name, String type) throws IOException {
		buildProperty(name, type, null, null);
	}
	
	protected void buildProperty(String name, String type, String analyzer) throws IOException {
		buildProperty(name, type, analyzer, null);
	}
	
	protected void buildProperty(String name, String type, String analyzer, String search_analyzer) throws IOException {
		builder.startObject(name);
		if(type != null) builder.field("type", type);
		if(analyzer != null) builder.field("analyzer", analyzer);
		if(search_analyzer != null) builder.field("search_analyzer", search_analyzer);
		builder.endObject();
	}
	
	
}
