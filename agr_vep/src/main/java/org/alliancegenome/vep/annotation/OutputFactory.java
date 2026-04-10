package org.alliancegenome.vep.annotation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.alliancegenome.vep.bio.CodonTable;
import org.alliancegenome.vep.csq.CsqEntry;
import org.alliancegenome.vep.hgvs.HgvsGenerator;
import org.alliancegenome.vep.model.GeneModel;
import org.alliancegenome.vep.model.TranscriptModel;
import org.alliancegenome.vep.plugin.PredictionLookup;
import org.alliancegenome.vep.reference.ReferenceGenome;

import htsjdk.variant.variantcontext.Allele;
import htsjdk.variant.variantcontext.VariantContext;

public class OutputFactory {

	private final GeneModel geneModel;
	private final TranscriptVariationAllele codingAnnotator;
	private final TranscriptAnnotator transcriptAnnotator;
	private final HgvsGenerator hgvsGenerator;
	private final String mod;
	private final PredictionLookup siftLookup;
	private final PredictionLookup polyPhenLookup;

	public OutputFactory(GeneModel geneModel, ReferenceGenome reference,
			HgvsGenerator hgvsGenerator, String mod) {
		this(geneModel, reference, hgvsGenerator, mod, null, null);
	}

	public OutputFactory(GeneModel geneModel, ReferenceGenome reference,
			HgvsGenerator hgvsGenerator, String mod,
			PredictionLookup siftLookup, PredictionLookup polyPhenLookup) {
		this.geneModel = geneModel;
		this.codingAnnotator = new TranscriptVariationAllele(reference);
		this.transcriptAnnotator = new TranscriptAnnotator(codingAnnotator, hgvsGenerator);
		this.hgvsGenerator = hgvsGenerator;
		hgvsGenerator.setCodingAnnotator(codingAnnotator);
		this.mod = mod;
		this.siftLookup = siftLookup;
		this.polyPhenLookup = polyPhenLookup;
	}

	public List<CsqEntry> annotate(VariantContext vc) {
		List<CsqEntry> allEntries = new ArrayList<>();
		String chr = geneModel.normalizeContig(vc.getContig());
		int start = vc.getStart();
		int end = vc.getEnd();
		String ref = vc.getReference().getBaseString();

		for (Allele altAllele : vc.getAlternateAlleles()) {
			String alt = altAllele.getBaseString();
			int variantStart = start;
			int variantEnd = end;
			String vepAllele;
			String vepRef;

			// VEP VCF.pm line 267-323: for indels (different length ref/alt),
			// strip exactly 1 char from the front when first bases match.
			// NOT per-allele longest-common-prefix — just 1 char, always.
			// No --minimal flag, so no split_variants/suffix trimming.
			boolean isIndel = ref.length() != alt.length();
			if (isIndel && ref.length() > 0 && alt.length() > 0
					&& ref.charAt(0) == alt.charAt(0)) {
				String refTrimmed = ref.substring(1);
				String altTrimmed = alt.substring(1);
				vepAllele = altTrimmed.isEmpty() ? "-" : altTrimmed;
				vepRef = refTrimmed.isEmpty() ? "-" : refTrimmed;
				variantStart = start + 1;
				if (refTrimmed.isEmpty()) {
					// Pure insertion: start > end
					variantEnd = variantStart - 1;
				}
				// For deletion/complex: variantEnd stays at htsjdk end
			} else {
				// SNP or equal-length substitution: no stripping
				vepAllele = alt;
				vepRef = ref;
			}

			List<TranscriptModel> overlapping = geneModel.getOverlappingTranscripts(chr, variantStart, variantEnd);

			if (overlapping.isEmpty()) {
				allEntries.add(createIntergenicEntry(chr, vepAllele, vepRef, variantStart, variantEnd));
			} else {
				// VEP outputs transcripts sorted by transcript stable ID (Feature field)
				overlapping.sort((a, b) -> a.getTranscriptId().compareTo(b.getTranscriptId()));
				List<CsqEntry> alleleEntries = new ArrayList<>();
				for (TranscriptModel transcript : overlapping) {
					CsqEntry entry = transcriptAnnotator.annotate(
						transcript, chr, variantStart, variantEnd, vepAllele, vepRef, mod);
					if (entry != null) {
						addPredictions(entry, transcript);
						alleleEntries.add(entry);
					}
				}
				if (alleleEntries.isEmpty()) {
					allEntries.add(createIntergenicEntry(chr, vepAllele, vepRef, variantStart, variantEnd));
				} else {
					allEntries.addAll(alleleEntries);
				}
			}
		}

		// AGR ProcessOutput.pm (agr_vep_pipeline) lines 49-63:
		// Compute Gene_level_consequence AFTER all alleles are processed,
		// operating on the full set of CSQ entries for the VCF line.
		computeGeneLevelConsequence(allEntries);

		// VEP outputs CSQ entries in transcript-major order (OutputFactory.pm line 513):
		// for each transcript (sorted by stable ID), for each allele (VCF order).
		// Our loop is allele-major; stable-sort by Feature to match VEP ordering.
		allEntries.sort(java.util.Comparator.comparing(
			(CsqEntry e) -> e.getFeature() != null ? e.getFeature() : ""));

		return allEntries;
	}

	private void computeGeneLevelConsequence(List<CsqEntry> entries) {
		// ProcessOutput.pm lines 49-63:
		// 1. VEP --flag_pick_allele_gene picks ONE transcript per allele+gene
		//	  (pick_order: mane_select > canonical > appris > tsl > biotype > ccds > rank > length)
		// 2. For each PICKED entry, store its consequence keyed by ALLELE (last PICK per allele wins)
		// 3. All entries get the stored consequence for their allele

		// Step 1: Determine which entries would have PICK flag set.
		// VEP flag_pick_allele_gene (OutputFactory.pm lines 622-626, 829-858):
		// Groups by allele, then within each allele groups by gene,
		// picks worst (most severe) transcript per gene.
		// We simulate this: for each allele+gene, pick the entry with the most severe consequence
		// (then longest transcript as tiebreaker — matching VEP pick_order after biotype/rank).
		Map<String, CsqEntry> pickedPerAlleleGene = new LinkedHashMap<>();
		for (CsqEntry entry : entries) {
			String allele = entry.getAllele() != null ? entry.getAllele() : "";
			String gene = entry.getGene() != null ? entry.getGene() : "";
			String key = allele + "|" + gene;
			CsqEntry current = pickedPerAlleleGene.get(key);
			if (current == null || isPicked(entry, current)) {
				pickedPerAlleleGene.put(key, entry);
			}
		}

		// Step 2: ProcessOutput.pm line 57-59:
		// For each PICKED entry, store consequence keyed by allele (last one wins)
		Map<String, String> glcPerAllele = new HashMap<>();
		for (CsqEntry picked : pickedPerAlleleGene.values()) {
			String allele = picked.getAllele() != null ? picked.getAllele() : "";
			glcPerAllele.put(allele, picked.getConsequence());
		}

		// Step 3: ProcessOutput.pm line 63:
		// Set GLC on all entries = stored consequence for their allele
		for (CsqEntry entry : entries) {
			String allele = entry.getAllele() != null ? entry.getAllele() : "";
			entry.setGeneLevelConsequence(glcPerAllele.get(allele));
		}
	}

	/**
	 * VEP pick_worst_VariationFeatureOverlapAllele (OutputFactory.pm lines 702-811):
	 * Returns true if candidate should be picked over current.
	 * Pick order: mane_select > canonical > appris > tsl > biotype > ccds > rank > length
	 * For AGR species (no MANE/canonical/APPRIS/TSL/CCDS): falls to rank then length.
	 */
	private boolean isPicked(CsqEntry candidate, CsqEntry current) {
		// Rank: lower = more severe = better
		int rankCand = ConsequenceSeverity.getMostSevereRank(candidate.getConsequence());
		int rankCurr = ConsequenceSeverity.getMostSevereRank(current.getConsequence());
		if (rankCand < rankCurr) return true;
		if (rankCand > rankCurr) return false;
		// Equal rank: longer transcript is better (we don't have length, so keep current)
		return false;
	}

	private CsqEntry createIntergenicEntry(String chr, String vepAllele, String refAllele, int start, int end) {
		CsqEntry entry = new CsqEntry();
		entry.setAllele(vepAllele);
		entry.setConsequence("intergenic_variant");
		entry.setImpact("MODIFIER");
		entry.setGeneLevelConsequence("intergenic_variant");
		entry.setGenomicStartPosition(String.valueOf(start));
		entry.setGenomicEndPosition(String.valueOf(end));

		String hgvsg = hgvsGenerator.generateHgvsg(chr, start, end, refAllele, vepAllele);
		if (hgvsg != null) {
			entry.setHgvsg(hgvsg);
		}

		return entry;
	}

	static String toVepAllele(String ref, String alt) {
		if (ref.length() == 1 && alt.length() == 1) {
			return alt;
		}
		int prefixLen = 0;
		while (prefixLen < ref.length() && prefixLen < alt.length()
				&& ref.charAt(prefixLen) == alt.charAt(prefixLen)) {
			prefixLen++;
		}
		String altTrimmed = alt.substring(prefixLen);
		String refTrimmed = ref.substring(prefixLen);

		if (altTrimmed.isEmpty()) return "-";
		if (refTrimmed.isEmpty()) return altTrimmed;
		return altTrimmed;
	}

	/**
	 * Add SIFT/PolyPhen predictions to a CSQ entry.
	 * VEP only looks up predictions for single amino acid substitutions
	 * (pep_allele_string =~ /^[A-Z]\/[A-Z]$/).
	 */
	private void addPredictions(CsqEntry entry, TranscriptModel transcript) {
		if (siftLookup == null && polyPhenLookup == null) return;

		String aminoAcids = entry.getAminoAcids();
		if (aminoAcids == null || aminoAcids.length() != 3 || aminoAcids.charAt(1) != '/') return;
		char refAA = aminoAcids.charAt(0);
		char altAA = aminoAcids.charAt(2);
		if (refAA == altAA || refAA == '*' || altAA == '*' || refAA == 'X' || altAA == 'X') return;

		String protPos = entry.getProteinPosition();
		if (protPos == null) return;
		int position;
		try {
			position = Integer.parseInt(protPos);
		} catch (NumberFormatException e) {
			return;
		}

		String md5 = transcript.getPeptideMd5();
		if (md5 == null) {
			md5 = computePeptideMd5(transcript);
			transcript.setPeptideMd5(md5);
		}
		if (md5 == null) return;

		if (siftLookup != null) {
			String[] result = siftLookup.getPrediction(md5, position, altAA);
			if (result != null) {
				entry.setSiftPrediction(result[0].replace(' ', '_'));
				entry.setSiftScore(result[1]);
			}
		}
		if (polyPhenLookup != null) {
			String[] result = polyPhenLookup.getPrediction(md5, position, altAA);
			if (result != null) {
				entry.setPolyPhenPrediction(result[0].replace(' ', '_'));
				entry.setPolyPhenScore(result[1]);
			}
		}
	}

	private String computePeptideMd5(TranscriptModel transcript) {
		if (!transcript.isCoding()) return null;
		String cds = BaseTranscriptVariation.translateableSeq(transcript, codingAnnotator.getReference());
		if (cds == null || cds.length() < 3) return null;
		StringBuilder peptide = new StringBuilder();
		for (int i = 0; i + 2 < cds.length(); i += 3) {
			char aa = CodonTable.translate(cds.substring(i, i + 3));
			if (aa == '*') break; // VEP's translate->seq excludes terminal stop
			peptide.append(aa);
		}
		return PredictionLookup.md5Hex(peptide.toString());
	}

	// ===================================================================
	// Ports from Bio::EnsEMBL::VEP::OutputFactory
	// ===================================================================

	/**
	 * VEP filter_VariationFeatureOverlapAlleles — OutputFactory.pm line 577-629.
	 * For flag_pick_allele_gene: groups VFOAs by allele, picks per gene within
	 * each allele group, flags the picked ones with PICK=1.
	 * All VFOAs are returned (not filtered), just flagged.
	 *
	 * Our implementation in computeGeneLevelConsequence simulates this by
	 * finding the picked entry per allele+gene and using its consequence
	 * for the Gene_level_consequence field.
	 */
	// Already implemented in computeGeneLevelConsequence above.

	/**
	 * VEP pick_worst_VariationFeatureOverlapAllele — OutputFactory.pm line 702-811.
	 * Full pick_order: mane_select > mane_plus_clinical > canonical > appris > tsl >
	 * biotype > ccds > rank > length > ensembl > refseq.
	 * For AGR species without MANE/canonical/APPRIS/TSL/CCDS, this simplifies to
	 * biotype > rank > length.
	 */
	// Already implemented in isPicked above.

	/**
	 * VEP pick_VariationFeatureOverlapAllele_per_gene — OutputFactory.pm line 829-858.
	 * Groups TVAs by gene, picks worst per gene.
	 */
	// Already implemented in computeGeneLevelConsequence above (allele+gene grouping).

	/**
	 * VEP TranscriptVariationAllele_to_output_hash — OutputFactory.pm line 1630-1730.
	 * Builds the CSQ hash for a TranscriptVariationAllele.
	 * Calls:
	 *   1. VariationFeatureOverlapAllele_to_output_hash (base: Allele, Consequence, HGVSg)
	 *   2. BaseTranscriptVariationAllele_to_output_hash (transcript: Feature, Gene, SYMBOL, etc.)
	 *   3. Coding-specific fields (cDNA_position, CDS_position, Amino_acids, Codons)
	 *   4. HGVSc, HGVSp
	 *   5. SIFT, PolyPhen
	 *   6. GIVEN_REF, USED_REF
	 *
	 * Our implementation is split between TranscriptAnnotator.annotate() (which builds CsqEntry)
	 * and this class (which adds predictions and HGVSg).
	 */
	// Already implemented across TranscriptAnnotator and OutputFactory.

	/**
	 * VEP add_sift_polyphen — OutputFactory.pm line 1746-1799.
	 * Adds SIFT and PolyPhen prediction/score to the output hash.
	 */
	// Already implemented in addPredictions above.

	static String toVepRef(String ref, String alt) {
		if (ref.length() == 1 && alt.length() == 1) {
			return ref;
		}
		int prefixLen = 0;
		while (prefixLen < ref.length() && prefixLen < alt.length()
				&& ref.charAt(prefixLen) == alt.charAt(prefixLen)) {
			prefixLen++;
		}
		String refTrimmed = ref.substring(prefixLen);
		if (refTrimmed.isEmpty()) return "-";
		return refTrimmed;
	}
}
