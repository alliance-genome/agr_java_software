package org.alliancegenome.cacher;

import org.alliancegenome.cache.CacheAlliance;
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
        assertEquals(CacheAlliance.ALLELE.getCacheName(), "geneAllele");
        assertEquals(CacheAlliance.TAXON.getCacheName(), "geneAlleleTaxon");
        assertEquals(CacheAlliance.SPECIES.getCacheName(), "geneAlleleSpecies");


    }

}
