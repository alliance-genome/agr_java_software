package org.alliancegenome.cacher;

import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cache.CacheService;

public class TestInfinispan {

	public static void main(String[] args) {
		
		CacheService cacheService = new CacheService();
		
		//List<String> list = cacheService.getAllKeys(CacheAlliance.DISEASE_ANNOTATION_GENE_LEVEL_GENE_DISEASE);

		
		String item = cacheService.getCacheEntry("DOID:612", CacheAlliance.ACCESSION_MAP, String.class);
		
		
		System.out.println(item);
	}

}
