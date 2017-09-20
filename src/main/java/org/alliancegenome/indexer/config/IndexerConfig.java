package org.alliancegenome.indexer.config;

import org.alliancegenome.indexer.indexers.DiseaseAnnotationIndexer;
import org.alliancegenome.indexer.indexers.DiseaseIndexer;
import org.alliancegenome.indexer.indexers.GeneIndexer;
import org.alliancegenome.indexer.indexers.GoIndexer;
import org.alliancegenome.indexer.schema.mappings.DiseaseMappings;
import org.alliancegenome.indexer.schema.mappings.GeneMappings;
import org.alliancegenome.indexer.schema.mappings.GoMappings;

public enum IndexerConfig {

	GeneIndexer("gene", GeneIndexer.class, GeneMappings.class, 2000, 6, 1000),
	DiseaseIndexer("disease", DiseaseIndexer.class, DiseaseMappings.class, 15000, 2, 100),
	DiseaseAnnotationIndexer("diseaseAnnotation", DiseaseAnnotationIndexer.class, DiseaseMappings.class, 500, 2, 100),
	GoIndexer("go", GoIndexer.class, GoMappings.class, 2500, 2, 100),;

	private String typeName;
	private Class<?> indexClazz;
	private Class<?> mappingsClazz;
	private int fetchChunkSize;
	private int threadCount;
	private int bufferSize;

	IndexerConfig(String typeName, Class<?> indexClazz, Class<?> mappingsClazz, int fetchChunkSize, int threadCount, int bufferSize) {
		this.typeName = typeName;
		this.indexClazz = indexClazz;
		this.mappingsClazz = mappingsClazz;
		this.fetchChunkSize = fetchChunkSize;
		this.threadCount = threadCount;
		this.bufferSize = bufferSize;
	}

	public String getTypeName() {
		return typeName;
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
	public int getThreadCount() {
		return threadCount;
	}
	public int getBufferSize() {
		return bufferSize;
	}

}
