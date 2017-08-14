package org.alliancegenome.indexer.schema.mappings;

import java.io.IOException;

import org.alliancegenome.indexer.schema.Mappings;

public class SearchableItemMappings extends Mappings {

	public SearchableItemMappings(Boolean pretty) {
		super(pretty);
	}

	public void buildMappings(boolean enclose) {
		try {
			
			if(enclose) builder.startObject();
			else {
				builder.startObject("mappings");
				builder.startObject("searchable_item");
			}
			
				builder.startObject("properties");
				
				buildProperty("primaryId", "keyword");
				buildProperty("taxonId", "keyword");
				buildProperty("geneSynopsisUrl", "keyword");
				buildProperty("geneLiteratureUrl", "keyword");
				buildProperty("soTermId", "keyword");
				buildProperty("secondaryIds", "keyword");
				buildProperty("geneSynopsis", "text");
				buildProperty("description", "text");
				
				buildProperty("href", "text", "symbols");
				buildProperty("id", "text", "symbols");
				buildProperty("systematicName", "text", "symbols");
				buildProperty("external_ids", "text", "symbols");
				
				
				buildGenericField("name", "text", null, true, true, true);
				
				buildGenericField("symbol", "text", "symbols", false, true, true);
				buildGenericField("synonyms", "text", "symbols", false, true, true);
				
				buildGenericField("name_key", "text", "symbols", false, true, false);
		
				buildCrossReferencesField();
				buildGenomeLocationsField();
				buildMetaDataField();
				
				buildGenericField("category", "keyword", null, true, false, true);
				buildGenericField("gene_biological_process", "text", null, true, false, true);
				buildGenericField("gene_molecular_function", "text", null, true, false, true);
				buildGenericField("gene_cellular_component", "text", null, true, false, true);
		
				buildGenericField("species", "text", null, false, false, true);
				buildGenericField("go_type", "text", null, false, false, true);
				buildGenericField("do_type", "text", null, false, false, true);
				buildGenericField("do_species", "text", null, false, false, true);
				buildGenericField("go_species", "text", null, false, false, true);
				
				buildGenericField("go_genes", "text", "symbols", false, false, true);
				buildGenericField("do_genes", "text", "symbols", false, false, true);
		
				buildDiseasesField();
				
				builder.endObject();
				
			if(enclose) builder.endObject();
			else {
				builder.endObject();
				builder.endObject();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	

	private void buildDiseasesField() throws IOException {
		builder.startObject("diseases");
			builder.startObject("properties");
				buildProperty("do_id", "text", "symbols");
				buildProperty("do_name", "text");
				buildProperty("dataProvider", "text");
				buildProperty("associationType", "text");
				builder.startObject("evidence");
					builder.startObject("properties");
						buildProperty("evidenceCode", "text");
						builder.startObject("pubs");
							builder.startObject("properties");
								buildProperty("pubmedId", "text");
								buildProperty("publicationModId", "text");
							builder.endObject();
						builder.endObject();
					builder.endObject();
				builder.endObject();
				builder.startObject("doIdDisplay");
					builder.startObject("properties");
						buildProperty("displayId", "text");
						buildProperty("url", "text");
						buildProperty("prefix", "text");
					builder.endObject();
				builder.endObject();
			builder.endObject();
		builder.endObject();
	}

	private void buildMetaDataField() throws IOException {
		builder.startObject("metaData");
		builder.startObject("properties");
		buildProperty("dateProduced", "date");
		buildProperty("dataProvider", "keyword");
		buildProperty("release", "keyword");
		builder.endObject();
		builder.endObject();
	}

	private void buildGenomeLocationsField() throws IOException {
		builder.startObject("genomeLocations");
		builder.startObject("properties");
		buildProperty("assembly", "keyword");
		buildProperty("startPosition", "integer");
		buildProperty("endPosition", "integer");
		buildProperty("chromosome", "keyword");
		buildProperty("strand", "keyword");
		builder.endObject();
		builder.endObject();
	}

	private void buildCrossReferencesField() throws IOException {
		builder.startObject("crossReferences");
		builder.startObject("properties");
		buildProperty("dataProvider", "keyword");
		buildProperty("id", "keyword");
		builder.endObject();
		builder.endObject();
	}
	
	private void buildGenericField(String name, String type, String analyzer, boolean symbol, boolean autocomplete, boolean raw) throws IOException {
		builder.startObject(name);
		if(type != null) builder.field("type", type);
		if(analyzer != null) builder.field("analyzer", analyzer);
		if(symbol || autocomplete || raw) {
			builder.startObject("fields");
			if(raw) buildProperty("raw", "keyword");
			if(symbol) buildGenericField("symbol", "text", "symbols", false, false, false);
			if(autocomplete) buildProperty("autocomplete", "text", "autocomplete", "autocomplete_search");
			builder.endObject();
		}
		builder.endObject();
	}
	
	private void buildProperty(String name, String type) throws IOException {
		buildProperty(name, type, null, null);
	}
	
	private void buildProperty(String name, String type, String analyzer) throws IOException {
		buildProperty(name, type, analyzer, null);
	}
	
	private void buildProperty(String name, String type, String analyzer, String search_analyzer) throws IOException {
		builder.startObject(name);
		if(type != null) builder.field("type", type);
		if(analyzer != null) builder.field("analyzer", analyzer);
		if(search_analyzer != null) builder.field("search_analyzer", search_analyzer);
		builder.endObject();
	}

}
