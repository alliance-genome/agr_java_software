package org.alliancegenome.cacher.config;

import org.alliancegenome.cacher.cachers.db.AlleleDBCacher;
import org.alliancegenome.cacher.cachers.db.ExpressionDBCacher;
import org.alliancegenome.cacher.cachers.db.GeneDBCacher;
import org.alliancegenome.cacher.cachers.db.GenePhenotypeDBCacher;

public enum DBCacherConfig {

    AlleleDBCacher("alleleDBCacher", AlleleDBCacher.class, Caches.AlleleCache),
    //GeneDBCacher("geneDBCacher", GeneDBCacher.class, Caches.GeneCache),
    GenePhenotypeDBCacher("genePhenotypeDBCacher", GenePhenotypeDBCacher.class, Caches.GenePhenotypeCache),
    //GeneExpressionDBCacher("geneExpressionDBCacher", ExpressionDBCacher.class, Caches.GeneExpressionCache),
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
