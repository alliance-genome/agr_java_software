package org.alliancegenome.api.tests.integration;

import org.alliancegenome.cache.repository.SiteMapCacheManager;
import org.junit.Test;

import jakarta.inject.Inject;

public class SiteMapGenIT {

	@Inject
	private SiteMapCacheManager smca;
	
	@Test
	public void testSiteMap() {

		//smca.cacheSiteMap("disease");
		smca.getGenesKeys();
		//smca.cacheSiteMap("allele");

	}

}
