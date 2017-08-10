package org.alliancegenome.indexer.config;

import org.alliancegenome.indexer.indexers.DiseaseIndexer;
import org.alliancegenome.indexer.indexers.GeneIndexer;
import org.alliancegenome.indexer.indexers.SearchableItemIndexer;
import org.alliancegenome.indexer.mapping.DiseaseMapping;
import org.alliancegenome.indexer.mapping.GeneMapping;
import org.alliancegenome.indexer.mapping.GoMapping;
import org.alliancegenome.indexer.mapping.SearchableItemMapping;

public enum IndexerConfig {

	GeneIndexer("gene", GeneIndexer.class, GeneMapping.class, 10000),
	DiseaseIndexer("disease", DiseaseIndexer.class, DiseaseMapping.class, 10000),
	GoIndexer("go", DiseaseIndexer.class, GoMapping.class, 10000),
	SearchableItemIndexer("searchable_item", SearchableItemIndexer.class, SearchableItemMapping.class, 1000)
	;

	private int fetchChunkSize;
	private Class<?> indexClazz;
	private Class<?> mappingClazz;
	private String indexName;
	private long commitFreq;
	private long fetchSize;

	IndexerConfig(String indexName, Class<?> indexClazz, Class<?> mappingClazz, int fetchChunkSize) {
		this.indexName = indexName;
		this.indexClazz = indexClazz;
		this.mappingClazz = mappingClazz;
		this.fetchChunkSize = fetchChunkSize;
	}

	public int getFetchChunkSize() {
		return fetchChunkSize;
	}
	public Class<?> getIndexClazz() {
		return indexClazz;
	}
	public Class<?> getMappingClazz() {
		return mappingClazz;
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
