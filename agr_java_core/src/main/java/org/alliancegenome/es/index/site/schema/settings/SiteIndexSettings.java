package org.alliancegenome.es.index.site.schema.settings;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import org.alliancegenome.es.index.site.schema.Settings;
import org.alliancegenome.neo4j.repository.indexer.GeneIndexerRepository;
import org.elasticsearch.common.xcontent.XContentBuilder;

public class SiteIndexSettings extends Settings {

    public SiteIndexSettings(Boolean pretty) {
        super(pretty);
    }

    // Used for the settings for site_index
    public void buildSettings() throws IOException {
        builder.startObject();
            builder.startObject("index")
                .field("max_result_window", "15000")
                .field("mapping.total_fields.limit","2000")
                .field("number_of_replicas", "0")
                .field("number_of_shards", "4");
                buildAnalysis(false);
            builder.endObject();
        builder.endObject();
    }

    // Used for taking snapshots
    public void buildRepositorySettings(String bucketName) throws IOException {
        builder.startObject()
                .field("bucket", bucketName)
                .field("compress", true)
            .endObject();
    }

}
