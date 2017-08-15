package org.alliancegenome.indexer.config;

import org.alliancegenome.indexer.indexers.DiseaseIndexer;
import org.alliancegenome.indexer.indexers.GeneIndexer;
import org.alliancegenome.indexer.indexers.SearchableItemIndexer;
import org.alliancegenome.indexer.schema.mappings.DiseaseMappings;
import org.alliancegenome.indexer.schema.mappings.GeneMappings;
import org.alliancegenome.indexer.schema.mappings.GoMappings;
import org.alliancegenome.indexer.schema.mappings.SearchableItemMappings;
import org.alliancegenome.indexer.schema.settings.DiseaseSettings;
import org.alliancegenome.indexer.schema.settings.GeneSettings;
import org.alliancegenome.indexer.schema.settings.GoSettings;
import org.alliancegenome.indexer.schema.settings.SearchableItemSettings;

public enum IndexerConfig {

	GeneIndexer("gene", GeneIndexer.class, GeneMappings.class, GeneSettings.class, 100),
	DiseaseIndexer("disease", DiseaseIndexer.class, DiseaseMappings.class, DiseaseSettings.class, 100),
	GoIndexer("go", DiseaseIndexer.class, GoMappings.class, GoSettings.class, 100),
	SearchableItemIndexer("searchable_item", SearchableItemIndexer.class, SearchableItemMappings.class, SearchableItemSettings.class, 5000)
	;

	private int fetchChunkSize;
	private Class<?> indexClazz;
	private Class<?> settingsClazz;
	private Class<?> mappingsClazz;
	private String indexName;
	private long commitFreq;
	private long fetchSize;

	IndexerConfig(String indexName, Class<?> indexClazz, Class<?> mappingsClazz, Class<?> settingsClazz, int fetchChunkSize) {
		this.indexName = indexName;
		this.indexClazz = indexClazz;
		this.settingsClazz = settingsClazz;
		this.mappingsClazz = mappingsClazz;
		this.fetchChunkSize = fetchChunkSize;
	}

	public int getFetchChunkSize() {
		return fetchChunkSize;
	}
	public Class<?> getIndexClazz() {
		return indexClazz;
	}
	public Class<?> getSettingsClazz() {
		return settingsClazz;
	}
	public Class<?> getMappingsClazz() {
		return mappingsClazz;
	}
	public String getIndexName() {
		return indexName;
	}
	public long getCommitFreq() {
		return commitFreq;
	}
	public long getFetchSize() {
		return fetchSize;
	}
}
