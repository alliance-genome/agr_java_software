package org.alliancegenome.indexer.schema.settings;

import org.alliancegenome.indexer.schema.Settings;

import java.io.IOException;

public class DiseaseSettings  extends Settings {

    public DiseaseSettings(Boolean pretty) {
        super(pretty);
    }

    public void buildSettings(boolean enclose) throws IOException {
        if(enclose) builder.startObject();
        else builder.startObject("settings");
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

}
