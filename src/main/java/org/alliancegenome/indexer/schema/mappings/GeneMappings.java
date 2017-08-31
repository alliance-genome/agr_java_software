package org.alliancegenome.indexer.schema.mappings;

import java.io.IOException;

import org.alliancegenome.indexer.schema.Mappings;

public class GeneMappings extends Mappings {
	
	public GeneMappings(Boolean pretty) {
		super(pretty);
	}

	public void buildMappings() {
		try {

			builder.startObject();
				builder.startObject("properties");

					buildProperty("id", "text", "symbols");
					buildProperty("systematicName", "text", "symbols");
					buildCrossReferencesField();
					buildMetaDataField();
					buildGenericField("category", "keyword", null, true, false, true);
					
					buildProperty("description", "text");
					//buildDiseasesField();
					buildProperty("external_ids", "text", "symbols");
					buildProperty("geneLiteratureUrl", "keyword");
					buildProperty("geneSynopsis", "text");
					buildProperty("geneSynopsisUrl", "keyword");
					buildGenericField("gene_biological_process", "text", null, true, false, true);
					buildGenericField("gene_cellular_component", "text", null, true, false, true);
					buildGenericField("gene_molecular_function", "text", null, true, false, true);
					buildGenomeLocationsField();
					buildProperty("href", "text", "symbols");
					buildGenericField("name", "text", null, true, true, true);
					buildGenericField("name_key", "text", "symbols", false, true, false);
					// Orthology
					buildProperty("primaryId", "keyword");
					// Release
					buildProperty("secondaryIds", "keyword");
					buildProperty("soTermId", "keyword");
					// So Term Name
					buildGenericField("species", "text", null, false, false, true);
					buildGenericField("symbol", "text", "symbols", false, true, true);
					buildGenericField("synonyms", "text", "symbols", false, true, true);
					buildProperty("taxonId", "keyword");
					
				builder.endObject();
			builder.endObject();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
//	private void buildDiseasesField() throws IOException {
//		builder.startObject("diseases");
//			builder.startObject("properties");
//				buildProperty("do_id", "text", "symbols");
//				buildProperty("do_name", "text");
//				buildProperty("dataProvider", "text");
//				buildProperty("associationType", "text");
//				builder.startObject("evidence");
//					builder.startObject("properties");
//						buildProperty("evidenceCode", "text");
//						builder.startObject("pubs");
//							builder.startObject("properties");
//								buildProperty("pubmedId", "text");
//								buildProperty("publicationModId", "text");
//							builder.endObject();
//						builder.endObject();
//					builder.endObject();
//				builder.endObject();
//				builder.startObject("doIdDisplay");
//					builder.startObject("properties");
//						buildProperty("displayId", "text");
//						buildProperty("url", "text");
//						buildProperty("prefix", "text");
//					builder.endObject();
//				builder.endObject();
//			builder.endObject();
//		builder.endObject();
//	}
	
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
}
