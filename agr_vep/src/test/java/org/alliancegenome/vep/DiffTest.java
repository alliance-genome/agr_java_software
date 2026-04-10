package org.alliancegenome.vep;

import static org.junit.Assert.*;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

import org.alliancegenome.vep.annotation.VariantAnnotator;
import org.alliancegenome.vep.csq.CsqEntry;
import org.junit.BeforeClass;
import org.junit.Test;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.vcf.VCFFileReader;

/**
 * Field-by-field comparison test against Perl VEP output.
 * Reads input VCF and expected VEP output from diff/ directory,
 * runs our pipeline, compares each CSQ field per-entry.
 *
 * Files expected:
 *   src/test/resources/{MOD}/diff/input.vcf   — VCF input records to test
 *   src/test/resources/{MOD}/diff/expected.vcf — Perl VEP output for those records
 */
public class DiffTest {

	private static final String[] FIELD_NAMES = {
		"Allele", "Consequence", "IMPACT", "SYMBOL", "Gene", "Feature_type", "Feature",
		"BIOTYPE", "EXON", "INTRON", "HGVSc", "HGVSp", "cDNA_position", "CDS_position",
		"Protein_position", "Amino_acids", "Codons", "Existing_variation", "DISTANCE",
		"STRAND", "FLAGS", "Gene_level_consequence", "SYMBOL_SOURCE", "HGNC_ID",
		"GIVEN_REF", "USED_REF", "BAM_EDIT", "SOURCE", "HGVS_OFFSET", "HGVSg",
		"PolyPhen_prediction", "PolyPhen_score", "SIFT_prediction", "SIFT_score",
		"transcript_name", "Genomic_end_position", "Genomic_start_position", "GFF_SOURCE"
	};

	private static Map<String, VariantAnnotator> annotators = new HashMap<>();

	private static final String[] DIFF_MODS = {"SGD", "WB", "ZFIN"};

	@BeforeClass
	public static void setUp() throws Exception {
		for (String mod : DIFF_MODS) {
			File inputFile = new File(TestHelper.getTestResourceDir() + "/" + mod + "/diff/input.vcf");
			if (inputFile.exists()) {
				annotators.put(mod, TestHelper.createAnnotator(mod));
			}
		}
	}

	@Test
	public void testSgdDiff() throws Exception {
		runDiff("SGD");
	}

	@Test
	public void testWbDiff() throws Exception {
		runDiff("WB");
	}

	@Test
	public void testZfinDiff() throws Exception {
		runDiff("ZFIN");
	}

	private void runDiff(String mod) throws Exception {
		File inputFile = new File(TestHelper.getTestResourceDir() + "/" + mod + "/diff/input.vcf");
		File expectedFile = new File(TestHelper.getTestResourceDir() + "/" + mod + "/diff/expected.vcf");
		if (!inputFile.exists() || !expectedFile.exists()) {
			org.junit.Assume.assumeTrue("Diff files not found for " + mod, false);
			return;
		}

		VariantAnnotator annotator = annotators.get(mod);
		assertNotNull("No annotator for " + mod, annotator);

		// Load expected CSQ entries keyed by chr:pos:ref:alts:feature:allele
		// Use gene (field 4) as secondary key to disambiguate entries with same
		// Feature but different genes (e.g., chrmt nested genes sharing RefSeq:NC_001224.1)
		Map<String, String[]> expected = loadExpectedCsq(expectedFile);

		// Run our pipeline and compare
		int[] fieldMismatch = new int[FIELD_NAMES.length];
		int totalEntries = 0;
		int matchedEntries = 0;
		int entryCountMismatch = 0;
		List<String> sampleErrors = new ArrayList<>();

		try (VCFFileReader reader = new VCFFileReader(inputFile, false)) {
			for (VariantContext vc : reader) {
				List<CsqEntry> entries = annotator.annotate(vc);
				String chr = vc.getContig();
				int pos = vc.getStart();
				// Include REF+ALTs in key to avoid collision between different
				// VCF lines at the same position (e.g., multi-allelic vs SNP)
				String vcfRef = vc.getReference().getBaseString();
				String vcfAlts = vc.getAlternateAlleles().stream()
					.map(Allele::getBaseString).collect(Collectors.joining(","));
				String keyPrefix = chr + ":" + pos + ":" + vcfRef + ":" + vcfAlts + ":";

				// Count expected entries for this VCF line
				int expectedCount = 0;
				for (String eKey : expected.keySet()) {
					if (eKey.startsWith(keyPrefix)) expectedCount++;
				}
				if (expectedCount != entries.size()) {
					entryCountMismatch++;
				}

				for (CsqEntry entry : entries) {
					totalEntries++;
					String key = keyPrefix + safe(entry.getFeature()) + ":" + safe(entry.getGene()) + ":" + safe(entry.getAllele());
					String[] exp = expected.get(key);
					if (exp == null) {
						// Try without gene (might differ)
						key = keyPrefix + safe(entry.getFeature()) + "::" + safe(entry.getAllele());
						exp = expected.get(key);
					}
					if (exp == null) {
						// Try without feature (intergenic)
						key = keyPrefix + "::" + safe(entry.getAllele());
						exp = expected.get(key);
					}
					if (exp == null) {
						if (sampleErrors.size() < 10) {
							sampleErrors.add("EXTRA: " + chr + ":" + pos + " " + entry.getFeature() + " " + entry.getAllele());
						}
						continue;
					}

					String[] actual = entryToFields(entry);
					boolean allMatch = true;
					for (int i = 0; i < Math.min(actual.length, exp.length); i++) {
						if (!safe(actual[i]).equals(safe(exp[i]))) {
							fieldMismatch[i]++;
							allMatch = false;
							if (sampleErrors.size() < 20) {
								sampleErrors.add(chr + ":" + pos + " " + FIELD_NAMES[i] +
									": java=" + safe(actual[i]) + " perl=" + safe(exp[i]));
							}
						}
					}
					if (allMatch) matchedEntries++;
				}
			}
		}

		// Report
		StringBuilder report = new StringBuilder();
		report.append(String.format("%s: %d/%d entries match (%.1f%%)\n",
			mod, matchedEntries, totalEntries, 100.0 * matchedEntries / Math.max(1, totalEntries)));
		report.append(String.format("Entry count mismatches: %d\n", entryCountMismatch));
		int totalFieldMismatches = 0;
		for (int i = 0; i < FIELD_NAMES.length; i++) {
			if (fieldMismatch[i] > 0) {
				report.append(String.format("  %-25s %d\n", FIELD_NAMES[i], fieldMismatch[i]));
				totalFieldMismatches += fieldMismatch[i];
			}
		}
		if (!sampleErrors.isEmpty()) {
			report.append("Sample errors:\n");
			for (String err : sampleErrors) {
				report.append("  " + err + "\n");
			}
		}

		assertEquals(report.toString(), 0, totalFieldMismatches);
	}

	private Map<String, String[]> loadExpectedCsq(File expectedFile) throws Exception {
		Map<String, String[]> result = new LinkedHashMap<>();
		try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(expectedFile))) {
			String line;
			while ((line = br.readLine()) != null) {
				if (line.startsWith("#")) continue;
				String[] cols = line.split("\t");
				if (cols.length < 8) continue;
				String chr = cols[0];
				String pos = cols[1];
				String info = cols[7];
				// Extract CSQ from INFO field
				int csqIdx = info.indexOf("CSQ=");
				if (csqIdx < 0) continue;
				String csqStr = info.substring(csqIdx + 4);
				// CSQ ends at next semicolon or end of string
				int semiIdx = csqStr.indexOf(';');
				if (semiIdx >= 0) csqStr = csqStr.substring(0, semiIdx);
				// May also end at tab (if genotype columns leaked through)
				int tabIdx = csqStr.indexOf('\t');
				if (tabIdx >= 0) csqStr = csqStr.substring(0, tabIdx);

				String vcfRef = cols[3];
				String vcfAlts = cols[4];
				for (String csq : csqStr.split(",")) {
					String[] fields = csq.split("\\|", -1);
					String feature = fields.length > 6 ? fields[6] : "";
					String gene = fields.length > 4 ? fields[4] : "";
					String allele = fields.length > 0 ? fields[0] : "";
					String key = chr + ":" + pos + ":" + vcfRef + ":" + vcfAlts + ":" + feature + ":" + gene + ":" + allele;
					result.put(key, fields);
				}
			}
		}
		return result;
	}

	private String[] entryToFields(CsqEntry entry) {
		String vcfStr = entry.toVcfString();
		return vcfStr.split("\\|", -1);
	}

	private String safe(String s) {
		return s != null ? s : "";
	}
}
