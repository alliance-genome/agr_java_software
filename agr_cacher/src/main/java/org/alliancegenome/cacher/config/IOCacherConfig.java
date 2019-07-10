package org.alliancegenome.cacher.config;

import org.alliancegenome.cacher.cachers.io.GeneAlleleIOCacher;
import org.alliancegenome.cacher.cachers.io.TaxonAlleleIOCacher;

public enum IOCacherConfig {

    GeneAlleleIOCacher("geneAlleleCacher", GeneAlleleIOCacher.class, Caches.AlleleCache, Caches.GeneAlleleCache),
    TaxonAlleleIOCacher("taxonAlleleCacher", TaxonAlleleIOCacher.class, Caches.AlleleCache, Caches.TaxonAlleleCache),
    ;

    private String cacherName;
    private Class<?> cacherClazz;
    private Caches inputCache;
    private Caches ouptutCache;

    IOCacherConfig(String cacherName, Class<?> indexClazz, Caches inputCache, Caches ouptutCache) {
        this.cacherName = cacherName;
        this.cacherClazz = indexClazz;
        this.inputCache = inputCache;
        this.ouptutCache = ouptutCache;
    }

    public String getCacherName() {
        return cacherName;
    }
    
    public Class<?> getCacherClazz() {
        return cacherClazz;
    }
    
    public Caches getInputCache() {
        return inputCache;
    }
    
    public Caches getOutputCache() {
        return ouptutCache;
    }
    

}
