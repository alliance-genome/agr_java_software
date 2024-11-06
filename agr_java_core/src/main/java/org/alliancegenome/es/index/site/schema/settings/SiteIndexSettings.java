package org.alliancegenome.es.index.site.schema.settings;

import java.io.IOException;

import org.alliancegenome.es.index.site.schema.Settings;

public class SiteIndexSettings extends Settings {

	private Integer shardCount;
	
	public SiteIndexSettings(Boolean pretty, Integer shardCount) {
		super(pretty);
		this.shardCount = shardCount;
	}

	// Used for the settings for site_index
	@Override
	public void buildSettings() throws IOException {
		builder.startObject();
			builder.startObject("index")
				.field("max_result_window", "250000")
				.field("mapping.total_fields.limit", "25000")
				.field("number_of_replicas", "0")
				.field("number_of_shards", shardCount);
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
