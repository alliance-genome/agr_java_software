package org.alliancegenome.indexer.config;

import org.alliancegenome.indexer.indexers.DiseaseIndexer;
import org.alliancegenome.indexer.indexers.GeneIndexer;
import org.alliancegenome.indexer.schema.mappings.DiseaseMappings;
import org.alliancegenome.indexer.schema.mappings.GeneMappings;
import org.alliancegenome.indexer.schema.mappings.GoMappings;

public enum TypeConfig {

	GeneIndexer("gene", GeneIndexer.class, GeneMappings.class, 100),
	DiseaseIndexer("disease", DiseaseIndexer.class, DiseaseMappings.class, 100),
	GoIndexer("go", DiseaseIndexer.class, GoMappings.class, 100),
	;

	private int fetchChunkSize;
	private Class<?> indexClazz;
	private Class<?> mappingsClazz;
	private String typeName;
	private long commitFreq;
	private long fetchSize;

	TypeConfig(String typeName, Class<?> indexClazz, Class<?> mappingsClazz, int fetchChunkSize) {
		this.typeName = typeName;
		this.indexClazz = indexClazz;
		this.mappingsClazz = mappingsClazz;
		this.fetchChunkSize = fetchChunkSize;
	}

	public int getFetchChunkSize() {
		return fetchChunkSize;
	}
	public Class<?> getIndexClazz() {
		return indexClazz;
	}
	public Class<?> getMappingsClazz() {
		return mappingsClazz;
	}
	public String getTypeName() {
		return typeName;
	}
	public long getCommitFreq() {
		return commitFreq;
	}
	public long getFetchSize() {
		return fetchSize;
	}
}
