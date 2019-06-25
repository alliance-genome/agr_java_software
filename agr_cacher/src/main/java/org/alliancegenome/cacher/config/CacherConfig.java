package org.alliancegenome.cacher.config;

import org.alliancegenome.cacher.cachers.GeneCacher;

public enum CacherConfig {

    // These numbers target a 10KB document batch size
    GeneCacher("gene", "persistent-file-store", GeneCacher.class, 1, 240),;
    //DiseaseIndexer("disease", DiseaseIndexer.class, 2, 150),
    //AlleleIndexer("allele", AlleleIndexer.class, 2, 743),
    //GoIndexer("go", GoIndexer.class, 2, 2300),;

    private String cacheName;
    private String cacheTemplateFile;
    private Class<?> indexClazz;
    private int threadCount;
    private int bufferSize;

    CacherConfig(String cacheName, String cacheTemplateFile, Class<?> indexClazz, int threadCount, int bufferSize) {
        this.cacheName = cacheName;
        this.cacheTemplateFile = cacheTemplateFile;
        this.indexClazz = indexClazz;
        this.threadCount = threadCount;
        this.bufferSize = bufferSize;
    }

    public String getCacheName() {
        return cacheName;
    }
    
    public String getCacheTemplate() {
        return cacheTemplateFile;
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
