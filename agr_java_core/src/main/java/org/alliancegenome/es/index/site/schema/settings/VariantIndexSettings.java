package org.alliancegenome.es.index.site.schema.settings;

import java.io.IOException;

import org.alliancegenome.es.index.site.schema.Settings;

public class VariantIndexSettings extends Settings {

    private int shardCount;
    
    public VariantIndexSettings(Boolean pretty, int shardCount) {
        super(pretty);
        this.shardCount = shardCount;
    }

    // Used for the settings for site_index
    public void buildSettings() throws IOException {
        builder.startObject();
        builder
            .startObject("index")
                .field("number_of_replicas", "0")
                .field("refresh_interval", "-1")
                .field("number_of_shards", "" + shardCount)
            .endObject();
        builder.endObject();
    }

}
