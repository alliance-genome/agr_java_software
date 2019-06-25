package org.alliancegenome.cacher.config;

import org.alliancegenome.cacher.cachers.db.GeneDBCacher;

public enum DBCacherConfig {

    GeneDBCacher("geneDBCacher", GeneDBCacher.class, Caches.GeneCache),
    ;

    private String cacherName;
    private Class<?> cacherClazz;
    private Caches cache;

    DBCacherConfig(String cacherName, Class<?> indexClazz, Caches cache) {
        this.cacherName = cacherName;
        this.cacherClazz = indexClazz;
        this.cache = cache;
    }

    public String getCacherName() {
        return cacherName;
    }
    
    public Class<?> getCacherClazz() {
        return cacherClazz;
    }
    
    public Caches getCache() {
        return cache;
    }

}
