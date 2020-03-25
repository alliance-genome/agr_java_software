package org.alliancegenome.api.tests.integration;

import javax.inject.Inject;

import org.alliancegenome.cache.repository.SiteMapCacheManager;
import org.junit.Test;

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
