package org.alliancegenome.vep;

import static org.junit.Assert.*;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alliancegenome.vep.annotation.VariantAnnotator;
import org.alliancegenome.vep.csq.CsqEntry;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;

/**
 * Consolidated consequence test — iterates over all MODs and consequence types.
 * Tests that our consequence classification matches VEP for sampled variants.
 */
@RunWith(Parameterized.class)
public class ConsequenceTest {

	private static final String[] MODS = {"SGD", "WB", "ZFIN", "RGD", "FB"};
	private static final String[] IGNORED_MODS = {"MGI", "HUMAN"};

	private static Map<String, VariantAnnotator> annotators = new HashMap<>();

	private final String mod;
	private final String consequence;
	private final String vcfFile;

	public ConsequenceTest(String mod, String consequence, String vcfFile) {
		this.mod = mod;
		this.consequence = consequence;
		this.vcfFile = vcfFile;
	}

	@BeforeClass
	public static void setUp() throws Exception {
		for (String mod : MODS) {
			annotators.put(mod, TestHelper.createAnnotator(mod));
		}
	}

	@Parameters(name = "{0}/{1}")
	public static Collection<Object[]> testFiles() {
		List<Object[]> params = new ArrayList<>();
		for (String mod : MODS) {
			File dir = new File(TestHelper.getTestResourceDir() + "/" + mod + "/consequence");
			if (dir.exists() && dir.listFiles() != null) {
				for (File f : dir.listFiles()) {
					if (f.getName().endsWith(".vcf")) {
						String cons = f.getName().replace(".vcf", "").replace("_AND_", "&");
						params.add(new Object[]{mod, cons, f.getAbsolutePath()});
					}
				}
			}
		}
		// Add ignored MODs with a placeholder so they show as skipped
		for (String mod : IGNORED_MODS) {
			File dir = new File(TestHelper.getTestResourceDir() + "/" + mod + "/consequence");
			if (dir.exists() && dir.listFiles() != null) {
				for (File f : dir.listFiles()) {
					if (f.getName().endsWith(".vcf")) {
						String cons = f.getName().replace(".vcf", "").replace("_AND_", "&");
						params.add(new Object[]{mod, cons, f.getAbsolutePath()});
					}
				}
			}
		}
		return params;
	}

	@Test
	public void testConsequence() throws Exception {
		// Skip ignored MODs (large GFFs)
		for (String ignored : IGNORED_MODS) {
			if (mod.equals(ignored)) {
				org.junit.Assume.assumeTrue("Skipping " + mod + " (large GFF)", false);
			}
		}

		VariantAnnotator annotator = annotators.get(mod);
		assertNotNull("No annotator for " + mod, annotator);

		int total = 0, matched = 0;
		List<String> failures = new ArrayList<>();

		try (VCFFileReader reader = new VCFFileReader(new File(vcfFile), false)) {
			for (VariantContext vc : reader) {
				total++;
				List<CsqEntry> entries = annotator.annotate(vc);
				boolean found = entries.stream().anyMatch(e ->
					e.getConsequence() != null && e.getConsequence().equals(consequence));
				if (found) {
					matched++;
				} else if (failures.size() < 5) {
					String actual = entries.stream().map(CsqEntry::getConsequence)
						.distinct().reduce((a, b) -> a + ", " + b).orElse("none");
					failures.add(String.format("%s:%d %s/%s -> %s",
						vc.getContig(), vc.getStart(),
						vc.getReference().getBaseString(),
						vc.getAlternateAlleles().get(0).getBaseString(), actual));
				}
			}
		}

		String msg = String.format("%s: %d/%d (%.0f%%)", consequence, matched, total,
			total > 0 ? 100.0 * matched / total : 0);
		if (!failures.isEmpty()) msg += "\n  " + String.join("\n  ", failures);
		assertTrue(msg, total == 0 || matched * 100 / total >= 50);
	}
}
