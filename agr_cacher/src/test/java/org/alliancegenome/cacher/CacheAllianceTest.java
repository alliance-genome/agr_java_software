package org.alliancegenome.cacher;

import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cacher.cachers.*;
import org.junit.Before;
import org.junit.Ignore;
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
    @Ignore
    public void diseaseCacher() {
        DiseaseCacher cacher = new DiseaseCacher();
        cacher.run();
    }

    @Test
    @Ignore
    public void phenotypeCacher() {
        GenePhenotypeCacher cacher = new GenePhenotypeCacher();
        cacher.run();
    }

    @Test
    @Ignore
    public void cacheEcoTerms() {
        EcoCodeHelperCacher cacher = new EcoCodeHelperCacher();
        cacher.run();
    }

    @Ignore
    @Test
    public void cacheOrthology() {
        GeneOrthologCacher cacher = new GeneOrthologCacher();
        cacher.run();
    }

    @Ignore
    @Test
    public void cacheAlleles() {
        AlleleCacher cacher = new AlleleCacher();
        cacher.run();
    }

    @Ignore
    @Test
    public void cachePhenotypes() {
        GenePhenotypeCacher cacher = new GenePhenotypeCacher();
        cacher.run();
    }


}
