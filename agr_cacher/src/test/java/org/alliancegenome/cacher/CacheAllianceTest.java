package org.alliancegenome.cacher;

import static org.junit.Assert.assertEquals;

import java.util.List;

import org.alliancegenome.cache.CacheAlliance;
import org.alliancegenome.cacher.cachers.DiseaseCacher;
import org.alliancegenome.cacher.cachers.EcoCodeCacher;
import org.alliancegenome.cacher.cachers.ExpressionCacher;
import org.alliancegenome.cacher.cachers.GeneOrthologCacher;
import org.alliancegenome.cacher.cachers.GenePhenotypeCacher;
import org.alliancegenome.cacher.cachers.InteractionCacher;
import org.alliancegenome.cacher.cachers.ModelCacher;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

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
		List<String> testGeneIDs = List.of("RGD:9294106",
				"ZFIN:ZDB-GENE-001212-1",
				"RGD:1624201",
				"ZFIN:ZDB-GENE-011101-3",
				"ZFIN:ZDB-GENE-020419-25",
				"RGD:11369140",
				"WB:WBGene00000913");
		//AlleleCacher cacher = new AlleleCacher(true, testGeneIDs);
		//cacher.run();
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
