package org.alliancegenome.indexer.config;

import org.alliancegenome.indexer.indexers.*;

public enum IndexerConfig {

    // These numbers target a 1MB document batch size
    // Buffer size = 1MB / average size
    // Bulk Action Size = Buffer Size
    // Bulk Size = 1MB
    // ConCurrent Reqests = Threads * 2
    // Document Stats as of 4/16/2021 ---- (doc count, min size, max size, average size) 
    GeneIndexer("gene", GeneIndexer.class, 2, 359, 359, 4, 1),          // (281090, 616, 156692, 2785)
    DatasetIndexer("dataset", DatasetIndexer.class, 2, 566, 566, 4, 1), // (8094,   358,  90542, 1765)
    DiseaseIndexer("disease", DiseaseIndexer.class, 2, 680, 680, 4, 1), // (10751,  191, 330099, 1472)
    AlleleIndexer("allele", AlleleIndexer.class, 2, 1517, 1517, 4, 1),  // (407492, 334,  69968,  659) 
    GoIndexer("go", GoIndexer.class, 2, 914, 914, 4, 1),                // (44087,  262, 460247, 1093)
    ModelIndexer("model", ModelIndexer.class, 2, 1426, 1426, 4, 1),     // (132454, 314,  18593,  701)
    // still not implemented  VariantIndexer("variant", VariantIndexer.class, 2, 3000, 400, 4, 4) 
    ;
    
    
    private String typeName;
    private Class<?> indexClazz;
    private int threadCount;
    private int bufferSize;
    private int bulkActions;
    private int concurrentRequests;
    private int bulkSize;

    IndexerConfig(String typeName, Class<?> indexClazz, int threadCount, int bufferSize, int bulkActions, int concurrentRequests, int bulkSize) {
        this.typeName = typeName;
        this.indexClazz = indexClazz;
        this.threadCount = threadCount;
        this.bufferSize = bufferSize;
        this.bulkActions = bulkActions;
        this.concurrentRequests = concurrentRequests;
        this.bufferSize = bulkSize;
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
    
    public int getBulkActions() {
        return bulkActions;
    }
    
    public int getConcurrentRequests() {
        return concurrentRequests;
    }
    
    public int getBulkSize() {
        return bulkSize;
    }

}
