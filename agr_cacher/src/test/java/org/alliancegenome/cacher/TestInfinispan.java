package org.alliancegenome.cacher;

import java.util.List;

import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.CacheService;
import org.alliancegenome.neo4j.entity.DiseaseAnnotation;

public class TestInfinispan {

	public static void main(String[] args) {
		
		CacheService cacheService = new CacheService();
		
		//List<String> list = cacheService.getAllKeys(CacheAlliance.DISEASE_ANNOTATION_GENE_LEVEL_GENE_DISEASE);

		
		List<DiseaseAnnotation> list = cacheService.getCacheEntries("DOID:4", CacheAlliance.DISEASE_ANNOTATION_GENE_LEVEL_GENE_DISEASE);
		
		
		System.out.println(list.size());
	}

}
