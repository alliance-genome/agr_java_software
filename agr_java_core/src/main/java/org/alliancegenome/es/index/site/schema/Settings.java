package org.alliancegenome.es.index.site.schema;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class Settings extends Builder {

	public Settings(Boolean pretty) {
		super(pretty);
	}

	public abstract void buildSettings() throws IOException;

	public void buildAnalysis(boolean skipSynonymFetching) throws IOException {
		builder
				.startObject("analysis")
					.startObject("char_filter")
						.startObject("zero_pad_numbers")
							.field("type", "pattern_replace")
							.field("pattern", "(\\d+)")
					        .field("replacement", "0000000$1")
						.endObject()
					.endObject()
					.startObject("analyzer")
						.startObject("default")
							.field("type", "custom")
							.field("tokenizer", "whitespace")
							.array("filter", new String[]{"lowercase", "apostrophe"})
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
						.startObject("keyword_autocomplete")
							.field("type", "custom")
							.field("tokenizer", "keyword")
							.array("filter", new String[]{"lowercase", "autocomplete_filter"})
						.endObject()
						.startObject("keyword_autocomplete_search")
							.field("type", "custom")
							.field("tokenizer", "keyword")
							.array("filter", new String[]{"lowercase"})
						.endObject()
						.startObject("symbols")
							.field("type", "custom")
							.field("tokenizer", "whitespace")
							.array("filter", new String[]{"lowercase"})
						.endObject()
						.startObject("generic_synonym")
							.field("type", "custom")
							.field("tokenizer", "whitespace")
							.array("filter", new String[]{"lowercase", "synonym_filter", "lowercase"})
						.endObject()
						.startObject("standard_bigrams")
							.field("type", "custom")
							.field("tokenizer", "standard")
							.array("filter", new String[]{"apostrophe", "lowercase", "bigram_filter"})
						.endObject()
						.startObject("standard_text")
							.field("type", "custom")
							.field("tokenizer", "standard")
							.array("filter", new String[]{"apostrophe", "lowercase"})
						.endObject()
						.startObject("classic_text")
							.field("type", "custom")
							.field("tokenizer", "classic")
							.array("filter", new String[]{"apostrophe", "lowercase"})
						.endObject()
						.startObject("letter_text")
							.field("type", "custom")
							.field("tokenizer", "letter")
							.array("filter", new String[]{"apostrophe", "lowercase"})
						.endObject()
						.startObject("html_smoosh")
							.field("type", "custom")
							.field("tokenizer", "keyword")
							.field("filter", "lowercase")
							.field("char_filter", "html_strip")
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
						.startObject("synonym_filter") //for any hand-crafted synonyms we need
							.field("type", "synonym")
							.array("synonyms", getSpeciesSynonyms(skipSynonymFetching))
						.endObject()
						.startObject("bigram_filter")
							.field("type", "shingle")
							.field("max_shingle_size", 2)
							.field("min_shingle_size", 2)
							.field("output_unigrams", "false")
						.endObject()
					.endObject()
					.startObject("normalizer")
						.startObject("lowercase")
							.field("type", "custom")
							.field("filter", "lowercase")
						.endObject()
						.startObject("smart_alpha_sort")
							.field("type", "custom")
							.field("filter", "lowercase")
							.field("char_filter", "zero_pad_numbers")
						.endObject()
					.endObject()
				.endObject();
	}

	public String[] getSpeciesSynonyms(Boolean skipSynonymFetching) {

		// Variant Indexer prototype doesn't initially have access to neo4j,
		// this code will go away when it has access and we'll get synonyms
		if (skipSynonymFetching) {
			return new String[0];
		}
		
		// TODO remove this hardcoding due to getting rid of NEO
		Map<String, Set<String>> synonymMap = Map.of(
			"Caenorhabditis elegans", Set.of("worm", "cel"),
			"Mus musculus", Set.of("mouse", "mmu"),
			"SARS-CoV-2", Set.of(
				"SARS-CoV-2",
				"Severe acute respiratory syndrome coronavirus 2",
				"SARS-CoV2",
				"sars cov 2",
				"SARS-2",
				"SARS2",
				"COVID",
				"COVID19",
				"COVID-19",
				"COVID-19 virus", "2019-nCoV", "HCoV-19",
				"Human coronavirus 2019"
			),
			"Xenopus laevis", Set.of("African clawed frog", "xbxl", "X.laevis", "X. laevis", "Bufo laevis", "Common platanna", "Platanna", "African claw-toed frog"),
			"Rattus norvegicus", Set.of("rat", "rno"),
			"Danio rerio", Set.of("zebrafish", "fish", "dre"),
			"Homo sapiens", Set.of("human", "hsa"),
			"Xenopus tropicalis", Set.of("Western clawed frog", "xbxt", "X.tropicalis", "X. tropicalis", "Tropical clawed frog", "Silurana tropicalis"),
			"Saccharomyces cerevisiae", Set.of("yeast", "sce"),
			"Drosophila melanogaster", Set.of("fly", "fruit fly", "dme")
		);

		Set<String> synonymMapping = new HashSet<>();

		for (String speciesName : synonymMap.keySet()) {
			StringBuilder sb = new StringBuilder();
			sb.append(speciesName.toLowerCase());
			sb.append(" => ");
			sb.append(
					synonymMap.get(speciesName).stream()
							.map(x -> x.replace("[", ""))
							.map(x -> x.replace("]", ""))
							.map(x -> x.replace("'", ""))
							.collect(Collectors.joining(","))
			);
			synonymMapping.add(sb.toString());
		}
		
		return synonymMapping.toArray(new String[0]);
	}

}
