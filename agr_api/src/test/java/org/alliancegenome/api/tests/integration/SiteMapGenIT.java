package org.alliancegenome.api.tests.integration;

import org.alliancegenome.api.application.SiteMapCacherApplication;
import org.junit.Test;

public class SiteMapGenIT {

    @Test
    public void testSiteMap() {
        SiteMapCacherApplication smca = new SiteMapCacherApplication();

        //smca.cacheSiteMap("disease");
        //smca.cacheSiteMap("gene");
        smca.cacheSiteMap("allele");

    }

}
