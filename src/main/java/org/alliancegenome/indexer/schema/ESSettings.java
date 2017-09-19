package org.alliancegenome.indexer.schema;

import java.io.IOException;

public class ESSettings extends Settings {

	public ESSettings(Boolean pretty) {
		super(pretty);
	}

	// Used for the settings for site_index
	public void buildSettings() throws IOException {
		builder.startObject();
		builder
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
			.endObject();
		builder.endObject();
	}
	
	// Used for taking snapshots
	public void buildRepositorySettings(String bucketName, String access_key, String secret_key) throws IOException {
		builder.startObject()
				.field("bucket", bucketName)
				.field("compress", true)
				.field("access_key", access_key)
				.field("secret_key", secret_key)
			.endObject();
	}
}
