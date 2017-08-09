package org.alliancegenome.indexer.config;

import org.alliancegenome.indexer.indexers.DiseaseIndexer;
import org.alliancegenome.indexer.indexers.GeneIndexer;
import org.alliancegenome.indexer.indexers.SearchableItemIndexer;
import org.alliancegenome.indexer.mapping.DiseaseMapping;
import org.alliancegenome.indexer.mapping.GeneMapping;
import org.alliancegenome.indexer.mapping.GoMapping;
import org.alliancegenome.indexer.mapping.SearchableItemMapping;

public enum IndexerConfig {

	GeneIndexer("gene", GeneIndexer.class, GeneMapping.class, 20000, 90000, 50000),
	DiseaseIndexer("disease", DiseaseIndexer.class, DiseaseMapping.class, 50000, 90000, 25000),
	GoIndexer("go", DiseaseIndexer.class, GoMapping.class, 50000, 90000, 25000),
	SearchableItemIndexer("searchable_item", SearchableItemIndexer.class, SearchableItemMapping.class, 50000, 75000, 25000)
	;

	private int chunkSize;
	private Class<?> indexClazz;
	private Class<?> mappingClazz;
	private String indexName;
	private int commitFreq;
	private int fetchSize;

	IndexerConfig(String indexName, Class<?> indexClazz, Class<?> mappingClazz, int chunkSize, int commitFreq, int fetchSize) {
		this.indexName = indexName;
		this.indexClazz = indexClazz;
		this.mappingClazz = mappingClazz;
		this.chunkSize = chunkSize;
		this.commitFreq = commitFreq;
		this.fetchSize = fetchSize;
	}

	public int getChunkSize() {
		return chunkSize;
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
	public int getFetchSize() {
		return fetchSize;
	}
}
