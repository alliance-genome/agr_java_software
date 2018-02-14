package org.alliancegenome.indexer.config;

import org.alliancegenome.indexer.indexers.DiseaseAnnotationIndexer;
import org.alliancegenome.indexer.indexers.DiseaseIndexer;
import org.alliancegenome.indexer.indexers.GeneIndexer;
import org.alliancegenome.indexer.indexers.GoIndexer;
import org.alliancegenome.indexer.schema.mappings.DiseaseMappings;
import org.alliancegenome.indexer.schema.mappings.GeneMappings;
import org.alliancegenome.indexer.schema.mappings.GoMappings;

public enum IndexerConfig {

    GeneIndexer("gene", GeneIndexer.class, GeneMappings.class, 4, 500),
    DiseaseIndexer("disease", DiseaseIndexer.class, DiseaseMappings.class, 2, 500),
    DiseaseAnnotationIndexer("diseaseAnnotation", DiseaseAnnotationIndexer.class, DiseaseMappings.class, 2, 300),
    GoIndexer("go", GoIndexer.class, GoMappings.class, 3, 500),;

    private String typeName;
    private Class<?> indexClazz;
    private Class<?> mappingsClazz;
    private int threadCount;
    private int bufferSize;

    IndexerConfig(String typeName, Class<?> indexClazz, Class<?> mappingsClazz, int threadCount, int bufferSize) {
        this.typeName = typeName;
        this.indexClazz = indexClazz;
        this.mappingsClazz = mappingsClazz;
        this.threadCount = threadCount;
        this.bufferSize = bufferSize;
    }

    public String getTypeName() {
        return typeName;
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
