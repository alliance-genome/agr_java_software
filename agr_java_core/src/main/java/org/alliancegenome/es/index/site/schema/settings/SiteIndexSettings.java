package org.alliancegenome.es.index.site.schema.settings;

import java.io.IOException;

import org.alliancegenome.es.index.site.schema.Settings;

public class SiteIndexSettings extends Settings {

    public SiteIndexSettings(Boolean pretty) {
        super(pretty);
    }

    // Used for the settings for site_index
    public void buildSettings() throws IOException {
        builder.startObject();
        builder
            .startObject("index")
                .field("max_result_window", "15000")
                .field("mapping.total_fields.limit","2000")
                .field("number_of_replicas", "0")
                .field("number_of_shards", "10")
                .startObject("analysis")
                    .startObject("analyzer")
                        .startObject("default")
                            .field("type", "custom")
                            .field("tokenizer", "whitespace")
                            .array("filter", new String[]{"lowercase","apostrophe"/*,"english_stemmer"*/})
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
                            .array("filter", new String[]{"lowercase","synonym_filter"})
                        .endObject()
                        .startObject("standard_bigrams")
                            .field("type","custom")
                            .field("tokenizer","standard")
                            .array("filter", new String[]{"apostrophe","standard","lowercase","bigram_filter"})
                         .endObject()
                        .startObject("standard_text")
                            .field("type","custom")
                            .field("tokenizer","standard")
                            .array("filter", new String[]{"apostrophe","standard","lowercase"})
                         .endObject()
                        .startObject("html_smoosh")
                            .field("type","custom")
                            .field("tokenizer","keyword")
                            .field("filter","lowercase")
                            .field("char_filter","html_strip")
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
                            .array("synonyms", new String[]{
                                    "homo sapiens => human, hsa",
                                    "rattus norvegicus => rat, rno",
                                    "mus musculus => mouse, mmu",
                                    "drosophila melanogaster => fly, fruit fly, dme",
                                    "caenorhabditis elegans => worm, cel",
                                    "saccharomyces cerevisiae => yeast. sce",
                                    "danio rerio => fish, zebrafish, dre"
                            })
                        .endObject()
                        .startObject("bigram_filter")
                           .field("type","shingle")
                           .field("max_shingle_size",2)
                           .field("min_shingle_size",2)
                           .field("output_unigrams","false")
                        .endObject()
                    .endObject()
                    .startObject("normalizer")
                        .startObject("lowercase")
                        .field("type", "custom")
                        .field("filter", "lowercase")
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
