package org.alliancegenome.api.tests.integration;

import org.alliancegenome.cache.repository.SiteMapCacheManager;
import org.junit.Test;

public class SiteMapGenIT {

    @Test
    public void testSiteMap() {
        SiteMapCacheManager smca = new SiteMapCacheManager();

        //smca.cacheSiteMap("disease");
        smca.getGenesKeys();
        //smca.cacheSiteMap("allele");

    }

}
