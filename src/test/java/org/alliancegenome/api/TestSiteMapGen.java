package org.alliancegenome.api;

import org.alliancegenome.api.application.SiteMapCacherApplication;

public class TestSiteMapGen {

    public static void main(String[] args) {
        SiteMapCacherApplication smca = new SiteMapCacherApplication();
        
        //smca.cacheSiteMap("disease");
        //smca.cacheSiteMap("gene");
        smca.cacheSiteMap("allele");

    }

}
