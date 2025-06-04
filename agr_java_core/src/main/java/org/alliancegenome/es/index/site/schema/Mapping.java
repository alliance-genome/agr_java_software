package org.alliancegenome.es.index.site.schema;

import java.io.IOException;

import org.elasticsearch.xcontent.XContentBuilder;

public class Mapping extends Builder {

	public Mapping(Boolean pretty) {
		super(pretty);
	}

	public void buildMapping() {
		try {
			builder.startObject().startObject("properties");
			buildSharedSearchableDocumentMappings();
			builder.endObject().endObject();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	protected void buildNestedDocument(String name) throws IOException {
		builder.startObject(name);
		builder.startObject("properties");
		//likely more fields than most will need, but the schema will be there for as many as are necessary
		buildSharedSearchableDocumentMappings();
		builder.endObject();
		builder.endObject();
	}

	protected void buildSharedSearchableDocumentMappings() throws IOException {
		// Not used fields
		//new FieldBuilder(builder, "age", "text").keyword().build();
		//new FieldBuilder(builder, "associationType", "text").symbol().autocomplete().keyword().standardText().build();
		//new FieldBuilder(builder, "biologicalProcess", "text").keyword().build();
		//new FieldBuilder(builder, "cellularComponent", "text").keyword().build();
		//new FieldBuilder(builder, "cellularComponentExpression", "text").keyword().build();
		//new FieldBuilder(builder, "cellularComponentExpressionWithParents", "text").keyword().build();
		//new FieldBuilder(builder, "cellularComponentExpressionAgrSlim", "text").keyword().build();
		//new FieldBuilder(builder, "description", "text").build();
		//new FieldBuilder(builder, "external_ids", "text").analyzer("symbols");
		//new FieldBuilder(builder, "geneLiteratureUrl", "keyword").build();
		//new FieldBuilder(builder, "id", "keyword");
		//new FieldBuilder(builder, "molecularFunction", "text").keyword().build();
		//new FieldBuilder(builder, "references.crossReferences", "nested").build();
		//new FieldBuilder(builder, "searchSymbol", "text").analyzer("symbols").autocomplete().keyword().keywordAutocomplete().sort().build();
		//new FieldBuilder(builder, "displayText", "text").keyword().sort().build();
		//new FieldBuilder(builder, "stage", "text").keyword().build();
		//new FieldBuilder(builder, "systematicName", "text").analyzer("symbols").build();
		//new FieldBuilder(builder, "taxonId", "keyword").build();

		// Allele Only Fields
		new FieldBuilder(builder, "alterationType", "text").keyword().build(); // Allele
		new FieldBuilder(builder, "allele.category", "text").keyword().build(); // Allele
		new FieldBuilder(builder, "constructs", "text").keyword().classicText().build(); // allele
		new FieldBuilder(builder, "constructExpressedComponent", "text").keyword().build(); // allele
		new FieldBuilder(builder, "constructKnockdownComponent", "text").keyword().build(); // allele
		new FieldBuilder(builder, "constructRegulatoryRegion", "text").keyword().build(); // allele
		new FieldBuilder(builder, "geneSynonyms", "text").keyword().build(); // allele
		new FieldBuilder(builder, "geneCrossReferences", "text").keyword().build(); // allele
		new FieldBuilder(builder, "molecularConsequence", "text").keyword().build(); // allele
		new FieldBuilder(builder, "symbolText", "text").keyword().standardText().build(); // allele
		new FieldBuilder(builder, "variants", "text").keyword().standardText().build(); // allele
		new FieldBuilder(builder, "variantType", "text").keyword().build(); // ??
		new FieldBuilder(builder, "variantSynonyms", "text").keyword().standardText().build(); // allele
		
		// Gene Only Fields
		new FieldBuilder(builder, "biologicalProcessAgrSlim", "text").keyword().build(); // gene
		new FieldBuilder(builder, "biologicalProcessWithParents", "text").keyword().build(); // gene
		new FieldBuilder(builder, "biotype0", "text").keyword().build(); // gene
		new FieldBuilder(builder, "biotype1", "text").keyword().build(); // gene
		new FieldBuilder(builder, "biotype2", "text").keyword().build(); // gene
		new FieldBuilder(builder, "biotypes", "text").keyword().letterText().build(); // gene
		new FieldBuilder(builder, "cellularComponentAgrSlim", "text").keyword().build(); // gene
		new FieldBuilder(builder, "cellularComponentWithParents", "text").keyword().build(); // gene
		new FieldBuilder(builder, "chromosomes", "text").keyword().build(); // gene
		new FieldBuilder(builder, "expressionStages", "text").keyword().standardText().build(); // gene
		new FieldBuilder(builder, "geneSynopsis", "text").build(); // gene
		new FieldBuilder(builder, "geneSynopsisUrl", "keyword").build(); // gene
		new FieldBuilder(builder, "molecularFunctionAgrSlim", "text").keyword().build(); // gene
		new FieldBuilder(builder, "molecularFunctionWithParents", "text").keyword().build(); // gene
		new FieldBuilder(builder, "soTermName", "text").keyword().letterText().build(); // gene
		new FieldBuilder(builder, "soTermId", "keyword").build(); // gene
		new FieldBuilder(builder, "strictOrthologySymbols", "text").keyword().autocomplete().build(); // gene
		
		// Dataset Only Fields
		new FieldBuilder(builder, "assays", "text").keyword().build(); // dataset
		new FieldBuilder(builder, "dataProvider", "text").keyword().build(); // dataset
		new FieldBuilder(builder, "sampleIds", "keyword").build(); // dataset
		new FieldBuilder(builder, "sex", "text").keyword().build(); // dataset
		new FieldBuilder(builder, "summary", "text").build(); // dataset
		
		new FieldBuilder(builder, "diseases", "text").keyword().build(); // gene, allele, model
		new FieldBuilder(builder, "diseasesAgrSlim", "text").keyword().build(); // gene, allele, model
		new FieldBuilder(builder, "diseasesWithParents", "text").keyword().build(); // gene, allele, model
		new FieldBuilder(builder, "phenotypeStatements", "text").keyword().build(); // gene, allele, model
		new FieldBuilder(builder, "phenotypeStatement", "text").keyword().sort().build(); // phenotype annotation

		new FieldBuilder(builder, "object.name", "text").keyword().sort().build(); // gene_disease_annotation, allele_disease_annotation, agm_disease_annotation
		new FieldBuilder(builder, "object.curie", "text").keyword().sort().build(); // gene_disease_annotation, allele_disease_annotation, agm_disease_annotation
		new FieldBuilder(builder, "subject.primaryExternalId", "text").keyword().sort().build(); // gene_disease_annotation, allele_disease_annotation, agm_disease_annotation
		
		new FieldBuilder(builder, "anatomicalExpression", "text").keyword().build(); // gene, dataset
		new FieldBuilder(builder, "whereExpressed", "text").keyword().build(); // gene, dataset

		new FieldBuilder(builder, "associatedSpecies", "text").keyword().synonym().sort().build(); // go, disease
		new FieldBuilder(builder, "model.agmFullName.displayText", "text").keyword().sort().build(); //
		new FieldBuilder(builder, "model.agmFullName.formatText", "text").keyword().sort().build(); //
		new FieldBuilder(builder, "definition", "text").standardText().build(); // go, disease
		
		new FieldBuilder(builder, "models", "text").keyword().autocomplete().build(); // gene, disease
		new FieldBuilder(builder, "secondaryIds", "keyword").build(); // gene, disease
		
		
		
		
		
		
		
		
		new FieldBuilder(builder, "alleles", "text").keyword().autocomplete().build(); // model, gene, disease

		new FieldBuilder(builder, "branch", "text").keyword().build(); // go
		new FieldBuilder(builder, "category", "keyword").symbol().autocomplete().keyword().build(); // ALL document must have
		
		new FieldBuilder(builder, "crossReferences", "text").keyword().classicText().build(); // allele, gene, dataset, disease


		new FieldBuilder(builder, "genes", "text").keyword().autocomplete().keywordAutocomplete().build(); // allele, model, go, disease
		new FieldBuilder(builder, "href", "keyword"); // go, dataset
		

		
		new FieldBuilder(builder, "name", "text").symbol().autocomplete().keyword().keywordAutocomplete().htmlSmoosh().standardBigrams().build(); // allele, gene, model, go, dataset, disease
		new FieldBuilder(builder, "nameText", "text").keyword().standardText().build(); // model
		new FieldBuilder(builder, "name_key", "text").analyzer("symbols").autocomplete().keyword().keywordAutocomplete().htmlSmoosh().standardBigrams().build(); // allele, gene, model, go, dataset, disease
		

		
		new FieldBuilder(builder, "subject.alleleSymbol.displayText", "text").keyword().sort().build(); // allele_disease_annotation
		new FieldBuilder(builder, "subject.geneSymbol.displayText", "text").keyword().sort().build(); // gene_disease_annotation
		new FieldBuilder(builder, "subject.name", "text").keyword().sort().build(); // agm_disease_annotation


		new FieldBuilder(builder, "popularity", "double").build(); // gene, model, dataset, disease
		new FieldBuilder(builder, "primaryKey", "keyword").build(); // allele, gene, model, go, dataset, disease
		new FieldBuilder(builder, "symbol", "text").analyzer("symbols").autocomplete().htmlSmoosh().keyword().keywordAutocomplete().sort().build(); // allele, gene
		
		
		new FieldBuilder(builder, "species", "text").keyword().synonym().sort().build(); // allele, gene, model, dataset
		new FieldBuilder(builder, "synonyms", "text").analyzer("symbols").autocomplete().keyword().keywordAutocomplete().htmlSmoosh().standardBigrams().build(); // gene, go, disease, model

		new FieldBuilder(builder, "geneMolecularInteraction.geneGeneAssociationObject.geneSymbol.displayText", "text").keyword().sort().build();
		new FieldBuilder(builder, "geneMolecularInteraction.interactorAType.name", "text").keyword().sort().build();
		new FieldBuilder(builder, "geneMolecularInteraction.interactorBType.name", "text").keyword().sort().build();
		new FieldBuilder(builder, "geneMolecularInteraction.detectionMethod.name", "text").keyword().sort().build();
		new FieldBuilder(builder, "geneMolecularInteraction.geneGeneAssociationObject.taxon.name", "text").keyword().sort().build();

		new FieldBuilder(builder, "geneGeneticInteraction.geneGeneAssociationObject.geneSymbol.displayText", "text").keyword().sort().build();
		new FieldBuilder(builder, "geneGeneticInteraction.interactorARole.name", "text").keyword().build();
		new FieldBuilder(builder, "geneGeneticInteraction.interactorBRole.name", "text").keyword().build();
		new FieldBuilder(builder, "geneGeneticInteraction.interactionType.name", "text").keyword().build();
		new FieldBuilder(builder, "geneGeneticInteraction.geneGeneAssociationObject.taxon.name", "text").keyword().sort().build();
	}
	
	public static class FieldBuilder {
		XContentBuilder builder;
		String name;
		String type;
		String analyzer;
		boolean index;
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
			this.index = true;
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

		public FieldBuilder notIndexed() {
			this.index = false;
			return this;
		}

		protected void buildProperty(String name, String type) throws IOException {
			buildProperty(name, type, null, null, null);
		}

		protected void buildProperty(String name, String type, String analyzer) throws IOException {
			buildProperty(name, type, analyzer, null, null);
		}

		protected void buildProperty(String name, String type, String analyzer, String searchAnalyzer, String normalizer) throws IOException {
			builder.startObject(name);
			if (type != null) {
				builder.field("type", type);
			}
			if (analyzer != null) {
				builder.field("analyzer", analyzer);
			}
			if (searchAnalyzer != null) {
				builder.field("search_analyzer", searchAnalyzer);
			}
			if (normalizer != null) {
				builder.field("normalizer", normalizer);
			}
			builder.endObject();
		}

		public void build() throws IOException {
			builder.startObject(name);
			if (type != null) {
				builder.field("type", type);
			}
			if (!index) {
				builder.field("index", false);
			}
			if (analyzer != null) {
				builder.field("analyzer", analyzer);
			}
			if (symbol || autocomplete || keyword || keywordAutocomplete || synonym || sort || standardText) {
				builder.startObject("fields");
				if (keyword) {
					buildProperty("keyword", "keyword");
				}
				if (keywordAutocomplete) {
					buildProperty("keywordAutocomplete", "text", "keyword_autocomplete", "keyword_autocomplete_search", null);
				}
				if (letterText) {
					buildProperty("letterText", "text", "letter_text", "default", null);
				}
				if (symbol) {
					buildProperty("symbol", "text", "symbols");
				}
				if (autocomplete) {
					buildProperty("autocomplete", "text", "autocomplete", "autocomplete_search", null);
				}
				if (classicText) {
					buildProperty("classicText", "text", "classic_text", "default", null);
				}
				if (synonym) {
					buildProperty("synonyms", "text", "generic_synonym", "autocomplete_search", null);
				}
				if (sort) {
					//buildProperty("sort", "keyword", null, null, "lowercase");
					buildProperty("sort", "keyword", null, null, "smart_alpha_sort");
				}
				if (htmlSmoosh) {
					buildProperty("htmlSmoosh", "text", "html_smoosh");
				}
				if (standardBigrams) {
					buildProperty("standardBigrams", "text", "standard_bigrams");
				}
				if (standardText) {
					buildProperty("standardText", "text", "standard_text", "default", null);
				}
				builder.endObject();
			}
			builder.endObject();
		}
	}

}
