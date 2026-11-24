package org.alliancegenome.core.es.util;

import java.io.IOException;
import java.util.List;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.core.es.schema.Mapping;
import org.alliancegenome.core.es.schema.SequenceSummaryMapping;
import org.alliancegenome.core.es.schema.Settings;
import org.alliancegenome.core.es.schema.VariantMapping;
import org.alliancegenome.core.es.schema.settings.SiteIndexSettings;
import org.alliancegenome.core.es.schema.settings.VariantIndexSettings;

public enum IndexName {

	SITE_INDEX("site_index", new SiteIndexSettings(true, ConfigHelper.getEsShardCount()), new Mapping(true)),
	SEARCH_RESULT("search_result", new SiteIndexSettings(true, ConfigHelper.getEsShardCount()), new Mapping(true)),
	VARIANT_SUMMARY("variant_summary", new VariantIndexSettings(true, ConfigHelper.getEsShardCount()), new VariantMapping(true)),
	SEQUENCE_SUMMARY("sequence_summary", new VariantIndexSettings(true, ConfigHelper.getEsShardCount()), new SequenceSummaryMapping(true)),
	;
	
	private String indexName;
	private Settings settings;
	private Mapping mapping;
	
	IndexName(String indexName, Settings settings, Mapping mapping) {
		this.indexName = indexName;
		this.settings = settings;
		try {
			this.settings.buildSettings();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.mapping = mapping;
		this.mapping.buildMapping();
	}
	
	public String getIndexName() {
		return indexName;
	}
	
	public Settings getSettings() {
		return settings;
	}
	
	public Mapping getMapping() {
		return mapping;
	}
	
	public static List<IndexName> getSiteIndexSet() {
		return List.of(SITE_INDEX, SEARCH_RESULT, VARIANT_SUMMARY, SEQUENCE_SUMMARY);
	}
	
	public static List<IndexName> getVariantIndexSet() {
		return List.of(SEARCH_RESULT, VARIANT_SUMMARY, SEQUENCE_SUMMARY);
	}
}
