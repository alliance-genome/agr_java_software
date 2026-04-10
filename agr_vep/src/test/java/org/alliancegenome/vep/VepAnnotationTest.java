package org.alliancegenome.vep;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.List;

import org.alliancegenome.vep.annotation.ConsequenceSeverity;
import org.alliancegenome.vep.annotation.OutputFactory;
import org.alliancegenome.vep.csq.CsqEntry;
import org.alliancegenome.vep.model.GeneModel;
import org.junit.BeforeClass;
import org.junit.Test;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;
import htsjdk.variant.variantcontext.VariantContextBuilder;

public class VepAnnotationTest {

	private static OutputFactory sgdAnnotator;
	private static OutputFactory wbAnnotator;
	private static GeneModel sgdGeneModel;
	private static GeneModel wbGeneModel;

	@BeforeClass
	public static void setUp() throws Exception {
		sgdAnnotator = TestHelper.createAnnotator("SGD");
		sgdGeneModel = TestHelper.loadGeneModel("SGD");
		wbAnnotator = TestHelper.createAnnotator("WB");
		wbGeneModel = TestHelper.loadGeneModel("WB");
	}

	private VariantContext makeVariant(String chr, int pos, String ref, String alt) {
		return new VariantContextBuilder()
			.chr(chr).start(pos).stop(pos + ref.length() - 1)
			.alleles(Arrays.asList(Allele.create(ref, true), Allele.create(alt)))
			.make();
	}

	private CsqEntry findByTranscript(List<CsqEntry> entries, String transcriptId) {
		return entries.stream().filter(e -> transcriptId.equals(e.getFeature())).findFirst().orElse(null);
	}

	@Test public void testSgd_synonymous() {
		CsqEntry e = findByTranscript(sgdAnnotator.annotate(makeVariant("chrI", 349, "C", "T")), "YAL069W_mRNA");
		assertNotNull(e);
		assertEquals("synonymous_variant", e.getConsequence());
		assertEquals("N", e.getAminoAcids());
		assertEquals("aaC/aaT", e.getCodons());
	}

	@Test public void testSgd_missense() {
		CsqEntry e = findByTranscript(sgdAnnotator.annotate(makeVariant("chrI", 356, "G", "A")), "YAL069W_mRNA");
		assertNotNull(e);
		assertEquals("missense_variant", e.getConsequence());
		assertEquals("V/M", e.getAminoAcids());
	}

	@Test public void testSgd_intergenic() {
		assertTrue(sgdAnnotator.annotate(makeVariant("chrI", 82, "C", "CT")).stream()
			.anyMatch(e -> "intergenic_variant".equals(e.getConsequence())));
	}

	@Test public void testSgd_stopGained() {
		CsqEntry e = findByTranscript(sgdAnnotator.annotate(makeVariant("chrI", 509, "G", "T")), "YAL069W_mRNA");
		assertNotNull(e);
		assertTrue(e.getConsequence().contains("stop_gained"));
	}

	@Test public void testSgd_contigNormalization() {
		assertTrue(sgdAnnotator.annotate(makeVariant("chrMt", 3957, "T", "A")).stream()
			.anyMatch(e -> e.getFeature() != null && !e.getFeature().isEmpty()));
	}

	@Test public void testSgd_intergenicWithTranscriptContext() {
		CsqEntry e = findByTranscript(sgdAnnotator.annotate(makeVariant("chrI", 7014, "G", "C")), "SGD:S000287741");
		assertNotNull(e);
		assertEquals("intergenic_variant", e.getConsequence());
	}

	@Test public void testSgd_transcriptCount() {
		assertTrue(sgdGeneModel.getTranscriptCount() > 10000);
		assertTrue(sgdGeneModel.getTranscriptCount() < 12000);
	}

	@Test public void testWb_transcriptCount() {
		assertTrue(wbGeneModel.getTranscriptCount() > 55000);
		assertTrue(wbGeneModel.getTranscriptCount() < 65000);
	}

	@Test public void testConsequenceOrdering_spliceRegionSynonymous() {
		assertEquals("splice_region_variant&synonymous_variant",
			ConsequenceSeverity.sortTerms("synonymous_variant&splice_region_variant"));
	}

	@Test public void testConsequenceOrdering_spliceCompound() {
		assertEquals("splice_region_variant&splice_polypyrimidine_tract_variant&intron_variant",
			ConsequenceSeverity.sortTerms("splice_polypyrimidine_tract_variant&splice_region_variant&intron_variant"));
	}

	@Test public void testConsequenceOrdering_stopGainedSplice() {
		assertEquals("stop_gained&splice_region_variant",
			ConsequenceSeverity.sortTerms("splice_region_variant&stop_gained"));
	}

	@Test public void testConsequenceOrdering_frameshiftStopLost() {
		assertEquals("frameshift_variant&stop_lost",
			ConsequenceSeverity.sortTerms("stop_lost&frameshift_variant"));
	}
}
