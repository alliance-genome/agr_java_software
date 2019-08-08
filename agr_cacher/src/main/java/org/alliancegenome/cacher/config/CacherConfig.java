package org.alliancegenome.cacher.config;

import org.alliancegenome.cacher.cachers.AlleleCacher;
import org.alliancegenome.cacher.cachers.DiseaseCacher;
import org.alliancegenome.cacher.cachers.ExpressionCacher;
import org.alliancegenome.cacher.cachers.GeneOrthologCacher;
import org.alliancegenome.cacher.cachers.GenePhenotypeCacher;
import org.alliancegenome.cacher.cachers.InteractionCacher;

public enum CacherConfig {

    AlleleDBCacher("geneAlleleCacher", AlleleCacher.class),
    //GeneDBCacher("geneDBCacher", GeneDBCacher.class),
    GenePhenotypeCacher("genePhenotypeCacher", GenePhenotypeCacher.class),
    GeneInteractionCacher("geneInteractionCacher", InteractionCacher.class),
    DiseaseCacher("diseaseCacher", DiseaseCacher.class),
    GeneExpressionCacher("geneExpressionDBCacher", ExpressionCacher.class),
    GeneOrthologCacher("geneOrthologCacher", GeneOrthologCacher.class),
    ;

    private String cacherName;
    private Class<?> cacherClass;

    CacherConfig(String cacherName, Class<?> cacherClazz) {
        this.cacherName = cacherName;
        this.cacherClass = cacherClazz;
    }

    public String getCacherName() {
        return cacherName;
    }

    public Class<?> getCacherClass() {
        return cacherClass;
    }

}
