package org.alliancegenome.indexer.config;

import org.alliancegenome.indexer.indexers.AlleleIndexer;
import org.alliancegenome.indexer.indexers.DiseaseIndexer;
import org.alliancegenome.indexer.indexers.GeneIndexer;
import org.alliancegenome.indexer.indexers.GoIndexer;
import org.alliancegenome.indexer.indexers.ModelIndexer;

public enum IndexerConfig {

    // These numbers target a 10KB document batch size
    GeneIndexer("searchable_item", GeneIndexer.class, 1, 240),
    DiseaseIndexer("searchable_item", DiseaseIndexer.class, 2, 150),
    AlleleIndexer("searchable_item", AlleleIndexer.class, 2, 743),
    GoIndexer("searchable_item", GoIndexer.class, 2, 2300),
    ModelIndexer("searchable_item", ModelIndexer.class, 2, 750); //just guessing at batching number

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
