package org.alliancegenome.vep.annotation;

import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.vep.model.TranscriptModel;

/**
 * Determines splice-related consequences based on variant position relative to intron/exon boundaries.
 * Logic derived from Ensembl VEP source:
 *   ensembl-variation/modules/Bio/EnsEMBL/Variation/BaseTranscriptVariationAllele.pm (_intron_effects)
 *   ensembl-variation/modules/Bio/EnsEMBL/Variation/Utils/VariationEffect.pm
 *
 * VEP evaluates each splice predicate INDEPENDENTLY — they are not mutually exclusive
 * (except where explicitly noted). A variant can simultaneously be splice_donor + 5th_base + intronic.
 */
public class SpliceAnnotator {

	public static class SpliceResult {
		private final List<String> spliceTerms;
		private final boolean intronic;

		public SpliceResult(List<String> spliceTerms, boolean intronic) {
			this.spliceTerms = spliceTerms;
			this.intronic = intronic;
		}

		public List<String> getSpliceTerms() { return spliceTerms; }
		public boolean isIntronic() { return intronic; }
	}

	public SpliceResult classifySplice(TranscriptModel transcript, int variantStart, int variantEnd) {
		List<String> spliceTerms = new ArrayList<>();
		boolean intronic = false;
		boolean positiveStrand = transcript.isPositiveStrand();

		// VEP checks each intron independently, accumulating flags across all introns
		boolean hasDonor = false;
		boolean hasAcceptor = false;
		boolean hasFifthBase = false;
		boolean hasDonorRegion = false;
		boolean hasPolypyrimidine = false;
		boolean hasSpliceRegion = false;

		for (int[] intron : transcript.getIntronIntervals()) {
			int intronStart = intron[0];
			int intronEnd = intron[1];

			// VEP skips frameshift introns (≤ 12bp) — BaseTranscriptVariation.pm line 1032-1035
			// These are flagged as _frameshift and skipped with `next` in _intron_effects
			int intronLength = intronEnd - intronStart + 1;
			if (intronLength <= 12 && overlap(variantStart, variantEnd, intronStart, intronEnd)) {
				continue;
			}

			// VEP _intron_effects: intronic = overlap with intron interior (positions +3 to end-2)
			// BaseTranscriptVariationAllele.pm line 144-149: includes insertion special case
			boolean insertion = variantStart == variantEnd + 1;
			if (overlap(variantStart, variantEnd, intronStart + 2, intronEnd - 2)
				|| (insertion && (variantStart == intronStart + 2 || variantEnd == intronEnd - 2))) {
				intronic = true;
			}

			// Donor splice site: first 2 bases of intron
			boolean startSpliceSite = overlap(variantStart, variantEnd, intronStart, intronStart + 1);
			boolean endSpliceSite = overlap(variantStart, variantEnd, intronEnd - 1, intronEnd);

			boolean isDonor = positiveStrand ? startSpliceSite : endSpliceSite;
			boolean isAcceptor = positiveStrand ? endSpliceSite : startSpliceSite;

			if (isDonor) hasDonor = true;
			if (isAcceptor) hasAcceptor = true;

			// 5th base: position +5 from donor end of intron
			// VEP: splice_donor_5th_base_variant is INDEPENDENT of splice_donor_variant
			boolean fifthBase = positiveStrand
				? overlap(variantStart, variantEnd, intronStart + 4, intronStart + 4)
				: overlap(variantStart, variantEnd, intronEnd - 4, intronEnd - 4);

			if (fifthBase) hasFifthBase = true;

			// Donor region: positions +3 to +6 from donor end of intron
			// VEP: splice_donor_region_variant returns 0 if splice_donor_5th_base_variant is true
			// But is INDEPENDENT of splice_donor_variant
			boolean donorRegion = positiveStrand
				? overlap(variantStart, variantEnd, intronStart + 2, intronStart + 5)
				: overlap(variantStart, variantEnd, intronEnd - 5, intronEnd - 2);

			if (donorRegion) hasDonorRegion = true;

			// Polypyrimidine tract: 15 bases upstream of acceptor (positions -17 to -3)
			// VEP: splice_polypyrimidine_tract_variant is INDEPENDENT of splice_acceptor_variant
			// Checked in both the interior and boundary intron loops in release/111
			boolean polypyrimidine = positiveStrand
				? overlap(variantStart, variantEnd, intronEnd - 16, intronEnd - 2)
				: overlap(variantStart, variantEnd, intronStart + 2, intronStart + 16);

			if (polypyrimidine) hasPolypyrimidine = true;

			// Splice region: 3-8 bases into intron OR 1-3 bases of exon
			// VEP _intron_overlap (VariationEffect.pm line 86-107): includes insertion cases
			// VEP: returns 0 if donor, acceptor, donor_region, or 5th_base is true
			if (!isDonor && !isAcceptor) {
				boolean spliceRegion =
					overlap(variantStart, variantEnd, intronStart + 2, intronStart + 7) ||
					overlap(variantStart, variantEnd, intronEnd - 7, intronEnd - 2) ||
					overlap(variantStart, variantEnd, intronStart - 3, intronStart - 1) ||
					overlap(variantStart, variantEnd, intronEnd + 1, intronEnd + 3) ||
					(insertion && (variantStart == intronStart || variantEnd == intronEnd
						|| variantStart == intronStart + 2 || variantEnd == intronEnd - 2));

				if (spliceRegion) hasSpliceRegion = true;
			}
		}

		// Build splice terms following VEP predicate independence
		if (hasAcceptor) spliceTerms.add("splice_acceptor_variant");
		if (hasDonor) spliceTerms.add("splice_donor_variant");
		if (hasFifthBase) spliceTerms.add("splice_donor_5th_base_variant");
		// donor_region excluded if 5th_base is true (VEP splice_donor_region_variant line 595)
		if (hasDonorRegion && !hasFifthBase) spliceTerms.add("splice_donor_region_variant");
		if (hasPolypyrimidine) spliceTerms.add("splice_polypyrimidine_tract_variant");
		// splice_region excluded if any of the above are true (VEP splice_region line 608-618)
		if (hasSpliceRegion && !hasDonor && !hasAcceptor && !hasFifthBase && !hasDonorRegion) {
			spliceTerms.add("splice_region_variant");
		}

		return new SpliceResult(spliceTerms, intronic);
	}

	/** @deprecated Use classifySplice() instead */
	public List<String> getSpliceConsequences(TranscriptModel transcript, int variantStart, int variantEnd) {
		return classifySplice(transcript, variantStart, variantEnd).getSpliceTerms();
	}

	private static boolean overlap(int s1, int e1, int s2, int e2) {
		return s1 <= e2 && e1 >= s2;
	}
}
