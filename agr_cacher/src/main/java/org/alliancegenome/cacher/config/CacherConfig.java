package org.alliancegenome.cacher.config;

import org.alliancegenome.cacher.cachers.*;

public enum CacherConfig {

    //AlleleCacher(AlleleCacher.class),
    //GeneCacher(GeneCacher.class),
    GenePhenotypeCacher(GenePhenotypeCacher.class),
    InteractionCacher(InteractionCacher.class),
    DiseaseCacher(DiseaseCacher.class),
    ExpressionCacher(ExpressionCacher.class),
    GeneOrthologCacher(GeneOrthologCacher.class),
    ModelCacher(ModelCacher.class),
    ClosureCacher(ClosureCacher.class),
    EcoCodeCacher(EcoCodeCacher.class),
    SiteMapCacher(SiteMapCacher.class),
    ;

    private String cacherName;
    private Class<?> cacherClass;

    CacherConfig(Class<?> cacherClazz) {
        this.cacherName = cacherClazz.getSimpleName();
        this.cacherClass = cacherClazz;
    }

    public String getCacherName() {
        return cacherName;
    }

    public Class<?> getCacherClass() {
        return cacherClass;
    }

}
