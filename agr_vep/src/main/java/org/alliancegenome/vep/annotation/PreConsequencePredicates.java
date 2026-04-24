package org.alliancegenome.vep.annotation;

import org.alliancegenome.vep.model.ExonModel;
import org.alliancegenome.vep.model.TranscriptModel;

/**
 * Port of Perl's _bvfo_preds (BaseVariationFeatureOverlapAllele.pm:454).
 *
 * Pre-computes boolean flags for a variant-transcript pair. These flags
 * are used by the include filter (Perl's _skip_oc) to gate which
 * consequence predicates are evaluated.
 *
 * Perl stores these as a hash ($bvfo_preds) with keys like 'exon', 'intron',
 * 'coding', etc. The include filter for each OverlapConsequence checks these
 * keys (e.g., splice_polypyrimidine_tract_variant requires exon=0, intron=1).
 */
public class PreConsequencePredicates {

	// Feature-level flags
	public final boolean withinFeature;
	public final boolean proteinCoding;

	// Overlap flags (computed from _overlapped_* methods)
	// Perl _overlapped_exons (BaseTranscriptVariation.pm:860): uses 12bp stretch
	// for transcripts with frameshift introns (abs(end-start) <= 12).
	public final boolean exon;
	// Perl _overlapped_introns: FULL intron span (start to end), NOT interior
	public final boolean intron;
	// Perl _overlapped_introns_boundary: intronStart-3..+7 OR intronEnd-7..+3
	public final boolean intronBoundary;

	// Coding region flags
	public final boolean coding;
	public final boolean utr;
	public final boolean nonCoding;

	// Allele-specific flags
	public final boolean snp;
	public final boolean insertion;
	public final boolean deletion;
	public final boolean increaseLength;
	public final boolean decreaseLength;

	private PreConsequencePredicates(boolean withinFeature, boolean proteinCoding,
			boolean exon, boolean intron, boolean intronBoundary,
			boolean coding, boolean utr, boolean nonCoding,
			boolean snp, boolean insertion, boolean deletion,
			boolean increaseLength, boolean decreaseLength) {
		this.withinFeature = withinFeature;
		this.proteinCoding = proteinCoding;
		this.exon = exon;
		this.intron = intron;
		this.intronBoundary = intronBoundary;
		this.coding = coding;
		this.utr = utr;
		this.nonCoding = nonCoding;
		this.snp = snp;
		this.insertion = insertion;
		this.deletion = deletion;
		this.increaseLength = increaseLength;
		this.decreaseLength = decreaseLength;
	}

	/**
	 * Perl _bvfo_preds + _bvf_preds combined.
	 * Computes all pre-consequence predicates for a variant-transcript pair.
	 *
	 * @param transcript The transcript model
	 * @param variantStart Genomic start (VEP convention: start > end for insertions)
	 * @param variantEnd Genomic end
	 * @param refAllele Ref allele string ("-" for insertions)
	 * @param vepAllele Alt allele string ("-" for deletions)
	 * @param overlapsCds Whether the variant maps to CDS coordinates (from BVT/geometric check)
	 * @param overlapsUtr Whether the variant overlaps UTR region
	 */
	public static PreConsequencePredicates compute(TranscriptModel transcript,
			int variantStart, int variantEnd,
			String refAllele, String vepAllele,
			boolean overlapsCds, boolean overlapsUtr) {

		// Perl _bvf_preds: allele-specific flags
		int refLen = "-".equals(refAllele) ? 0 : refAllele.length();
		int altLen = "-".equals(vepAllele) ? 0 : vepAllele.length();
		boolean snp = refLen == altLen && refLen > 0;
		boolean ins = altLen > refLen;
		boolean del = refLen > altLen;

		// Perl _bvfo_preds line 480: within_feature
		int minVf = Math.min(variantStart, variantEnd);
		int maxVf = Math.max(variantStart, variantEnd);
		boolean withinFeature = overlap(variantStart, variantEnd,
			transcript.getStart(), transcript.getEnd());

		// Perl line 494: protein_coding biotype
		boolean proteinCoding = "protein_coding".equals(transcript.getBiotype());

		boolean exonFlag = false;
		boolean intronFlag = false;
		boolean intronBoundaryFlag = false;
		boolean codingFlag = false;
		boolean utrFlag = false;
		boolean nonCodingFlag = false;

		if (withinFeature) {
			// Perl line 497-522: compute intron, intron_boundary, exon
			for (int[] intronInterval : transcript.getIntronIntervals()) {
				int iStart = intronInterval[0];
				int iEnd = intronInterval[1];

				// Skip invalid introns (RefSeq overlapping exons)
				if (iStart > iEnd) continue;

				// Perl _overlapped_introns (no-tree): FULL intron span
				if (overlap(minVf, maxVf, iStart, iEnd)) {
					intronFlag = true;
				}

				// Perl _overlapped_introns_boundary (no-tree):
				// intronStart-3..intronStart+7 OR intronEnd-7..intronEnd+3
				if (overlap(minVf, maxVf, iStart - 3, iStart + 7)
						|| overlap(minVf, maxVf, iEnd - 7, iEnd + 3)) {
					intronBoundaryFlag = true;
				}
			}

			// Perl _overlapped_exons (line 860-868): stretch by 12bp for
			// transcripts with frameshift introns
			int stretch = transcript.hasFrameshiftIntron() ? 12 : 0;
			for (ExonModel exonModel : transcript.getExons()) {
				// Perl _overlapped_exons_no_tree line 954:
				// overlap(minVf, maxVf, exon.start - stretch, exon.end + stretch)
				if (overlap(minVf, maxVf, exonModel.getStart() - stretch,
						exonModel.getEnd() + stretch)) {
					exonFlag = true;
					break;
				}
			}

			// Perl line 526-571: coding/utr/non_coding flags
			if (transcript.isCoding()) {
				int codingStart = transcript.getCdsStart();
				int codingEnd = transcript.getCdsEnd();
				if (codingStart > 0 && codingEnd > 0) {
					if (!overlap(minVf, maxVf, codingStart, codingEnd)) {
						// Completely within UTR
						utrFlag = true;
					} else {
						// Partially in UTR
						if (minVf < codingStart || maxVf > codingEnd) {
							utrFlag = true;
						}
						// Perl line 541-542: shortcut — exon=0 means non_coding
						if (!exonFlag) {
							nonCodingFlag = true;
						} else {
							codingFlag = overlapsCds;
							if (!codingFlag) {
								nonCodingFlag = true;
							}
						}
					}
				}
			} else {
				// Perl line 569-571: non-coding transcript
				nonCodingFlag = true;
			}

			// Also set utr from the caller's check (handles edge cases
			// where geometric UTR overlap differs from CDS boundary check)
			if (overlapsUtr) utrFlag = true;
		}

		return new PreConsequencePredicates(
			withinFeature, proteinCoding,
			exonFlag, intronFlag, intronBoundaryFlag,
			codingFlag, utrFlag, nonCodingFlag,
			snp, ins, del, ins, del);
	}

	/**
	 * Perl _skip_oc (BaseVariationFeatureOverlapAllele.pm:317).
	 * Checks whether a consequence predicate should be skipped based on
	 * the include filter. Returns true if the predicate should be SKIPPED.
	 *
	 * Perl logic:
	 *   for each key in include:
	 *     if include[key] == 0: skip if pred[key] EXISTS and pred[key] != 0
	 *     if include[key] == 1: skip unless pred[key] EXISTS and pred[key] == 1
	 *
	 * In Java, all pred keys always "exist" (they're boolean fields). For the
	 * include[key]==0 case (e.g., exon=0): skip if exon is true. For the
	 * include[key]==1 case (e.g., intron=1): skip unless intron is true.
	 *
	 * Special case: protein_coding=0 in Perl means "skip if protein_coding
	 * key EXISTS and is not 0". For non-coding transcripts, the protein_coding
	 * key doesn't exist in Perl's hash, so the check passes. In Java, we use
	 * !proteinCoding to match.
	 */
	public boolean shouldSkip(String soTerm) {
		return switch (soTerm) {
			// tier 1
			case "transcript_ablation" -> !deletion; // complete_overlap=1 handled by caller
			case "transcript_amplification" -> !increaseLength;

			// splice — require intron_boundary=1
			case "splice_acceptor_variant",
				 "splice_donor_variant",
				 "splice_donor_5th_base_variant",
				 "splice_region_variant",
				 "splice_donor_region_variant" -> !intronBoundary;

			// polypyrimidine — require exon=0 AND intron=1
			case "splice_polypyrimidine_tract_variant" -> exon || !intron;

			// coding consequences — require coding=1
			case "stop_gained", "stop_lost", "start_lost",
				 "protein_altering_variant",
				 "incomplete_terminal_codon_variant",
				 "start_retained_variant", "stop_retained_variant",
				 "synonymous_variant", "coding_sequence_variant" -> !coding;

			// coding + shape-specific
			case "frameshift_variant" -> !coding || snp;
			case "inframe_insertion" -> !coding || !insertion;
			case "inframe_deletion" -> !coding || !deletion;
			case "missense_variant" -> !coding || decreaseLength || increaseLength;

			// UTR — require exon=1 AND utr=1
			case "5_prime_UTR_variant", "3_prime_UTR_variant" -> !exon || !utr;

			// non-coding exon — require exon=1 AND protein_coding=0 AND within_feature=1
			case "non_coding_transcript_exon_variant" -> !exon || proteinCoding || !withinFeature;

			// intron — require intron=1
			case "intron_variant" -> !intron;

			// non-coding transcript — require protein_coding=0 AND within_feature=1
			case "non_coding_transcript_variant" -> proteinCoding || !withinFeature;

			// Don't skip anything else
			default -> false;
		};
	}

	/** Perl Utils.pm overlap(a, b, c, d) = (b >= c) && (a <= d) */
	private static boolean overlap(int a, int b, int c, int d) {
		return b >= c && a <= d;
	}
}
