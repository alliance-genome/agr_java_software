package org.alliancegenome.api.tests.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Comparator;
import java.util.List;

import org.alliancegenome.core.config.ConfigHelper;
import org.alliancegenome.neo4j.entity.node.Variant;
import org.alliancegenome.neo4j.repository.VariantRepository;
import org.junit.Before;
import org.junit.Test;

public class VariantIT {

	private VariantRepository variantRepository = new VariantRepository();

	@Before
	public void before() {
		ConfigHelper.init();
	}

	@Test
	public void checkVariantHgvsDuplication() {
		List<Variant> variants = variantRepository.getVariantsOfAllele("MGI:5295051");
		assertNotNull(variants);
		assertEquals(variants.size(), 2);
		variants.sort(Comparator.comparing(Variant::getName));
		Variant variant = variants.get(0);
		assertEquals(variants.size(), 2);
		String variantNames = String.join(",", variant.getHgvsG());
		List<String> expectedNames = List.of("(GRCm39)3:115711821_115711824delinsCCGC", "3:g.115711821_115711824delinsCCGC");
		expectedNames.forEach(name -> assertTrue(name + " does not exist", variantNames.contains(name)));
		String unexpectedName = "3:g.115711833T>C";
		assertFalse(unexpectedName + " exists but should not on this transcript", variantNames.contains(unexpectedName));

	}

}
