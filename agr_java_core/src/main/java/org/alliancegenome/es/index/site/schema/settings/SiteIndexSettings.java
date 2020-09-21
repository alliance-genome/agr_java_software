package org.alliancegenome.es.index.site.schema.settings;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.alliancegenome.es.index.site.schema.Settings;
import org.alliancegenome.neo4j.repository.GeneIndexerRepository;

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
                .field("number_of_shards", "4")
                .startObject("analysis")
                    .startObject("analyzer")
                        .startObject("default")
                            .field("type", "custom")
                            .field("tokenizer", "whitespace")
                            .array("filter", new String[]{"lowercase","apostrophe"})
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
                            .array("filter", new String[]{"lowercase","synonym_filter","lowercase"})
                        .endObject()
                        .startObject("standard_bigrams")
                            .field("type","custom")
                            .field("tokenizer","standard")
                            .array("filter", new String[]{"apostrophe","lowercase","bigram_filter"})
                         .endObject()
                        .startObject("standard_text")
                            .field("type","custom")
                            .field("tokenizer","standard")
                            .array("filter", new String[]{"apostrophe","lowercase"})
                         .endObject()
                         .startObject("classic_text")
                            .field("type","custom")
                            .field("tokenizer","classic")
                            .array("filter", new String[]{"apostrophe","lowercase"})
                         .endObject()
                        .startObject("letter_text")
                            .field("type","custom")
                            .field("tokenizer", "letter")
                            .array("filter", new String[]{"apostrophe","lowercase"})
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
                            .array("synonyms", getSpeciesSynonyms())
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
    public void buildRepositorySettings(String bucketName) throws IOException {
        builder.startObject()
                .field("bucket", bucketName)
                .field("compress", true)
            .endObject();
    }

    public String[] getSpeciesSynonyms() {

        GeneIndexerRepository geneIndexerRepository = new GeneIndexerRepository();
        Map<String, Set<String>> synonymMap = geneIndexerRepository.getSpeciesCommonNames();
        Set<String> synonymMapping = new HashSet<>();
        for (String speciesName : synonymMap.keySet()) {
            StringBuilder sb = new StringBuilder();
            sb.append(speciesName.toLowerCase());
            sb.append(" => ");
            sb.append(
                synonymMap.get(speciesName).stream()
                        .map(x -> x.replace("[",""))
                        .map(x -> x.replace("]",""))
                        .map(x -> x.replace("'",""))
                        .collect(Collectors.joining(","))
            );
            synonymMapping.add(sb.toString());
        }
        return synonymMapping.toArray(new String[0]);
    }
}
