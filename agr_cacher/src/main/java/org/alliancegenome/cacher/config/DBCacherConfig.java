package org.alliancegenome.cacher.config;

import org.alliancegenome.cacher.cachers.db.*;

public enum DBCacherConfig {

    AlleleDBCacher("geneAlleleCacher", AlleleDBCacher.class),
    //GeneDBCacher("geneDBCacher", GeneDBCacher.class),
    GenePhenotypeCacher("genePhenotypeCacher", GenePhenotypeDBCacher.class),
    GeneInteractionCacher("geneInteractionCacher", InteractionCacher.class),
    DiseaseCacher("diseaseCacher", DiseaseDBCacher.class),
    GeneExpressionCacher("geneExpressionDBCacher", ExpressionDBCacher.class),
    GeneOrthologCacher("geneOrthologCacher", GeneOrthologCacher.class),
    ;

    private String cacherName;
    private Class<?> cacherClass;

    DBCacherConfig(String cacherName, Class<?> cacherClazz) {
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
