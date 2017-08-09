package org.alliancegenome.indexer.mapping;

import java.io.IOException;

public class SearchableItemMapping extends Mapping {

	public SearchableItemMapping(Boolean pretty) {
		super(pretty);
	}

	public String buildMapping() {

		try {
			builder.startObject();
			settings();
			buildMappings();
			builder.endObject();

			return builder.string();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return "";
	}

	private void settings() throws IOException {

		builder
		.startObject("settings")
			.startObject("index")
				.field("max_result_window", "15000")
				.field("number_of_replicas", "0")
				.field("number_of_shards", "5")
				.startObject("analysis")
					.startObject("analyzer")
						.startObject("default")
							.field("type", "custom")
							.field("tokenizer", "whitespace")
							.array("filter", new String[]{"english_stemmer", "lowercase"})
						.endObject()
						.startObject("autocomplete")
							.field("type", "custom")
							.field("tokenizer", "whitespace")
							.array("filter", new String[]{"lowercase", "autocomplete_filter"})
						.endObject()
						.startObject("autocomplete_search")
							.field("type", "custom")
							.field("tokenizer", "whitespace")
							.array("filter", new String[]{"lowercase"})
						.endObject()
						.startObject("symbols")
							.field("type", "custom")
							.field("tokenizer", "whitespace")
							.array("filter", new String[]{"lowercase"})
						.endObject()
					.endObject()
					.startObject("filter")
						.startObject("english_stemmer")
							.field("type", "stemmer")
							.field("language", "english")
						.endObject()
						.startObject("autocomplete_filter")
							.field("type", "edge_ngram")
							.field("min_gram", "1")
							.field("max_gram", "20")
						.endObject()
					.endObject()
				.endObject()
			.endObject()
		.endObject();
	}

	private void buildMappings() throws IOException {
		
		builder.startObject("mappings");
		builder.startObject("searchable_item");
		builder.startObject("properties");
		
		buildGenericField("primaryId", "keyword", null, false, false, false);
		buildGenericField("taxonId", "keyword", null, false, false, false);
		buildGenericField("geneSynopsisUrl", "keyword", null, false, false, false);
		buildGenericField("geneLiteratureUrl", "keyword", null, false, false, false);
		buildGenericField("soTermId", "keyword", null, false, false, false);
		buildGenericField("secondaryIds", "keyword", null, false, false, false);
		buildGenericField("geneSynopsis", "text", null, false, false, false);
		buildGenericField("description", "text", null, false, false, false);
		
		buildGenericField("href", "text", "symbols", false, false, false);
		buildGenericField("id", "text", "symbols", false, false, false);
		buildGenericField("systematicName", "text", "symbols", false, false, false);
		buildGenericField("external_ids", "text", "symbols", false, false, false);
		
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
		builder.endObject();
		builder.endObject();
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
