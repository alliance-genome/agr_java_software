package org.alliancegenome.core.es.schema.settings;

import java.io.IOException;

import org.alliancegenome.core.es.schema.Settings;

public class VariantIndexSettings extends Settings {
	public static final int MAX_RESULT_WINDOW = 150000;

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
				.field("max_result_window", MAX_RESULT_WINDOW)
				.field("refresh_interval", "-1")
				.field("number_of_shards", "" + shardCount)
				.field("merge.scheduler.max_thread_count", "1")
				.field("merge.policy.segments_per_tier", "50")
				.field("merge.policy.floor_segment", "200mb")
				.field("translog.sync_interval", "60s")
				.field("translog.flush_threshold_size", "4096mb")
				.field("translog.durability", "async");
				//.field("codec", "best_compression");
			buildAnalysis(true);
			builder.endObject();
		builder.endObject();
	}

}
