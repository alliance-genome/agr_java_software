package org.alliancegenome.cacher.config;

import org.alliancegenome.cacher.cachers.EcoCodeCacher;
import org.alliancegenome.cacher.cachers.ExpressionCacher;
import org.alliancegenome.cacher.cachers.ModelCacher;
import org.alliancegenome.cacher.cachers.SiteMapCacher;

public enum CacherConfig {

	//AlleleCacher(AlleleCacher.class),
	//GeneCacher(GeneCacher.class),
	//GenePhenotypeCacher(GenePhenotypeCacher.class),
	//InteractionCacher(InteractionCacher.class),
	//GeneOrthologCacher(GeneOrthologCacher.class),
	//GeneParalogCacher(GeneParalogCacher.class),
	//ClosureCacher(ClosureCacher.class),
	//DiseaseCacher(DiseaseCacher.class),
	
	ExpressionCacher(ExpressionCacher.class),
	ModelCacher(ModelCacher.class),
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
