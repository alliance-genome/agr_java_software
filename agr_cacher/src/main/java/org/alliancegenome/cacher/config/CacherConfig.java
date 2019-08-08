package org.alliancegenome.cacher.config;

import org.alliancegenome.cacher.cachers.db.AlleleDBCacher;
import org.alliancegenome.cacher.cachers.db.DiseaseDBCacher;
import org.alliancegenome.cacher.cachers.db.ExpressionDBCacher;
import org.alliancegenome.cacher.cachers.db.GeneOrthologCacher;
import org.alliancegenome.cacher.cachers.db.GenePhenotypeDBCacher;
import org.alliancegenome.cacher.cachers.db.InteractionCacher;

public enum CacherConfig {

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
