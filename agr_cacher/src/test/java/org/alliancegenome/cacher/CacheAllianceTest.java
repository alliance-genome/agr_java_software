package org.alliancegenome.cacher;

import static org.junit.Assert.assertEquals;

import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cacher.cachers.*;
import org.junit.*;

public class CacheAllianceTest {
    @Before
    public void before() {
    }

    @Test
    public void getCacheNames() {
        assertEquals(CacheAlliance.ALLELE_GENE.getCacheName(), "allele_gene");
        assertEquals(CacheAlliance.ALLELE_SPECIES.getCacheName(), "allele_species");
    }

    @Test
    @Ignore
    public void diseaseCacher() {
        DiseaseCacher cacher = new DiseaseCacher();
        cacher.setUseCache(true);
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
    public void interactionCacher() {
        InteractionCacher cacher = new InteractionCacher();
        cacher.run();
    }

    @Test
    @Ignore
    public void cacheEcoTerms() {
        EcoCodeCacher cacher = new EcoCodeCacher();
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
    public void cacheExpression() {
        ExpressionCacher cacher = new ExpressionCacher();
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
    public void cacheModels() {
        ModelCacher cacher = new ModelCacher();
        cacher.run();
    }

    @Ignore
    @Test
    public void cachePhenotypes() {
        GenePhenotypeCacher cacher = new GenePhenotypeCacher();
        cacher.run();
    }


}
