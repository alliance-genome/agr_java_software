package org.alliancegenome.cacher;

import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cacher.cachers.DiseaseCacher;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CacheAllianceTest {
    @Before
    public void before() {
    }

    @Test
    public void getCacheNames() {
        assertEquals(CacheAlliance.GENE.getCacheName(), "gene");
        assertEquals(CacheAlliance.ALLELE.getCacheName(), "allele");
        assertEquals(CacheAlliance.ALLELE_TAXON.getCacheName(), "allele_taxon");
        assertEquals(CacheAlliance.ALLELE_SPECIES.getCacheName(), "allele_species");


    }

    @Test
    public void gett(){
        DiseaseCacher cacher = new DiseaseCacher();
        //cacher.run();
    }

}
