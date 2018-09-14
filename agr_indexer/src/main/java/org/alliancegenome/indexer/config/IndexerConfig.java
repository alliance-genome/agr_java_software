package org.alliancegenome.indexer.config;

import org.alliancegenome.indexer.indexers.*;

public enum IndexerConfig {

    // These numbers target a 100KB document batch size
    GeneIndexer("gene", GeneIndexer.class, 2, 2400),
    PhenotypeIndexer("termName", PhenotypeIndexer.class, 2, 155),
    PhenotypeAnnotationIndexer("phenotypeAnnotation", PhenotypeAnnotationIndexer.class, 2, 8300),
    DiseaseIndexer("disease", DiseaseIndexer.class, 2, 1500),
    DiseaseAnnotationIndexer("diseaseAnnotation", DiseaseAnnotationIndexer.class, 2, 6350),
    FeatureIndexer("feature", FeatureIndexer.class, 2, 7430),
    GoIndexer("go", GoIndexer.class, 2, 23000),;

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
