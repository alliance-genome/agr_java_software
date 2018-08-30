package org.alliancegenome.indexer.config;

import org.alliancegenome.indexer.indexers.*;

public enum IndexerConfig {

    GeneIndexer("gene", GeneIndexer.class, 4, 500),
    PhenotypeIndexer("termName", PhenotypeIndexer.class, 2, 100),
    PhenotypeAnnotationIndexer("phenotypeAnnotation", PhenotypeAnnotationIndexer.class, 2, 100),
    DiseaseIndexer("disease", DiseaseIndexer.class, 2, 100),
    DiseaseAnnotationIndexer("diseaseAnnotation", DiseaseAnnotationIndexer.class, 2, 300),
    FeatureIndexer("feature", FeatureIndexer.class, 2, 500),
    GoIndexer("go", GoIndexer.class, 3, 500),;

    private String typeName;
    private Class<?> indexClazz;
    private int threadCount;
    private int bufferSize;

    IndexerConfig(String typeName, Class<?> indexClazz, int threadCount, int bufferSize) {
        this.typeName = typeName;
        this.indexClazz = indexClazz;
        this.threadCount = threadCount;
        this.bufferSize = bufferSize;
    }

    public String getTypeName() {
        return typeName;
    }

    public Class<?> getIndexClazz() {
        return indexClazz;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public int getBufferSize() {
        return bufferSize;
    }

}
