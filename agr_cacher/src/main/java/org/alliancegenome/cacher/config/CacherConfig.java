package org.alliancegenome.cacher.config;

import org.alliancegenome.cacher.cachers.*;

public enum CacherConfig {

    AlleleDBCacher(AlleleCacher.class),
    //GeneDBCacher(GeneCacher.class),
    GenePhenotypeCacher(GenePhenotypeCacher.class),
    GeneInteractionCacher(InteractionCacher.class),
    DiseaseCacher(DiseaseCacher.class),
    GeneExpressionCacher(ExpressionCacher.class),
    GeneOrthologCacher(GeneOrthologCacher.class),
    EcoCodeDiseaseJoinCacher(EcoCodeHelperCacher.class),
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
