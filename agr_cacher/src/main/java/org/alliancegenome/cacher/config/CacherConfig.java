package org.alliancegenome.cacher.config;

import org.alliancegenome.cacher.cachers.AlleleCacher;
import org.alliancegenome.cacher.cachers.DiseaseCacher;
import org.alliancegenome.cacher.cachers.ExpressionCacher;
import org.alliancegenome.cacher.cachers.GeneOrthologCacher;
import org.alliancegenome.cacher.cachers.GenePhenotypeCacher;
import org.alliancegenome.cacher.cachers.InteractionCacher;

public enum CacherConfig {

    AlleleDBCacher(AlleleCacher.class),
    //GeneDBCacher(GeneCacher.class),
    GenePhenotypeCacher(GenePhenotypeCacher.class),
    GeneInteractionCacher(InteractionCacher.class),
    DiseaseCacher(DiseaseCacher.class),
    GeneExpressionCacher(ExpressionCacher.class),
    GeneOrthologCacher(GeneOrthologCacher.class),
    ;

    private String cacherName;
    private Class<?> cacherClass;

    CacherConfig(Class<?> cacherClazz) {
        this.cacherName = cacherClazz.getName();
        this.cacherClass = cacherClazz;
    }

    public String getCacherName() {
        return cacherName;
    }

    public Class<?> getCacherClass() {
        return cacherClass;
    }

}
