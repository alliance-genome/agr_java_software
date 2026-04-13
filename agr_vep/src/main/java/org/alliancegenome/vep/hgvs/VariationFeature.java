package org.alliancegenome.vep.hgvs;

import org.alliancegenome.vep.bio.Sequence;
import org.alliancegenome.vep.reference.ContigAccessionMap;
import org.alliancegenome.vep.reference.ReferenceGenome;

/**
 * Port of Bio::EnsEMBL::Variation::VariationFeature — HGVSg generation.
 * Uses Sequence.hgvsVariantNotation() and Sequence.formatHgvsString()
 * matching VEP's hgvs_genomic() line 1852-2000.
 */
public class VariationFeature {

	private final ContigAccessionMap contigMap;
	private final ReferenceGenome reference;

	public VariationFeature(ContigAccessionMap contigMap) {
		this(contigMap, null);
	}

	public VariationFeature(ContigAccessionMap contigMap, ReferenceGenome reference) {
		this.contigMap = contigMap;
		this.reference = reference;
	}

	/**
	 * VEP hgvs_genomic() line 1852-2000.
	 * Generates HGVSg notation for a variant.
	 */
	public String generate(String chr, int start, int end, String ref, String alt) {
		String accession = contigMap.getAccession(chr);
		if (accession == null) return null;

		// VEP line 1901-1902: get flank sequence for dup checking
		// We use the reference genome directly instead of a flank substring
		String refSequence = null;
		if (reference != null) {
			try {
				// Get enough flanking sequence for dup detection
				int flankStart = Math.max(1, start - 100);
				int flankEnd = end + 100;
				refSequence = reference.getSequence(chr, flankStart, flankEnd);
				// Adjust coordinates relative to flank
				int refStart = start - flankStart + 1;
				int refEnd = end - flankStart + 1;

				// VEP line 1972-1980: hgvs_variant_notation
				String checkAllele = "-".equals(alt) ? "" : alt;
				int refLength = "-".equals(ref) ? 0 : ref.length();

				Sequence.HgvsNotation notation = Sequence.hgvsVariantNotation(
					checkAllele,	// alt allele
					refSequence,	// reference sequence (flank)
					refStart,		// start in ref sequence
					refEnd,			// end in ref sequence
					start,			// display start (chromosomal)
					end,			// display end (chromosomal)
					-1				// dup lookup direction (preceding)
				);

				if (notation == null) return null;

				// VEP line 1986: _clip_alleles if type is delins
				if ("delins".equals(notation.type)) {
					notation = clipAlleles(notation);
				}

				// VEP line 1994: format_hgvs_string
				return Sequence.formatHgvsString(accession, "g", notation);

			} catch (Exception e) {
				// Fall through to simple generation
			}
		}

		// Fallback: simple generation without reference sequence
		return generateSimple(accession, start, end, ref, alt);
	}

	/**
	 * VEP _clip_alleles — VariationFeature.pm line 2003-2061.
	 * Clips common prefix/suffix from ref/alt for HGVS notation.
	 * Type determination matches VEP exactly (> check before del).
	 */
	private Sequence.HgvsNotation clipAlleles(Sequence.HgvsNotation n) {
		String checkRef = n.ref;
		String checkAlt = n.alt;
		int checkStart = n.start;
		int checkEnd = n.end;
		String preseq = "";

		// Line 2009-2015: prefix trim
		while (checkRef.length() > 0 && checkAlt.length() > 0
				&& checkRef.charAt(0) == checkAlt.charAt(0)) {
			preseq += checkRef.charAt(0);
			checkRef = checkRef.substring(1);
			checkAlt = checkAlt.substring(1);
			checkStart++;
		}

		// Line 2018-2023: suffix trim
		while (checkRef.length() > 0 && checkAlt.length() > 0
				&& checkRef.charAt(checkRef.length() - 1) == checkAlt.charAt(checkAlt.length() - 1)) {
			checkRef = checkRef.substring(0, checkRef.length() - 1);
			checkAlt = checkAlt.substring(0, checkAlt.length() - 1);
			checkEnd--;
		}

		// Line 2026-2027: empty to dash
		if (checkRef.isEmpty()) checkRef = "-";
		if (checkAlt.isEmpty()) checkAlt = "-";

		n.ref = checkRef;
		n.alt = checkAlt;
		n.start = checkStart;
		n.end = checkEnd;

		// Line 2029-2058: type determination
		if (checkRef.equals(checkAlt)) {
			n.type = "=";
		}
		// Line 2033-2038: 1-char substitution (includes ref>- for clipped deletions)
		else if (!checkRef.equals("-") && checkRef.length() == 1 && checkAlt.length() == 1
				&& !checkAlt.equals(checkRef)) {
			n.type = ">";
		}
		// Line 2041-2053: insertion from clipping
		else if (checkRef.equals("-") && checkAlt.length() >= 1) {
			String prevStr = preseq.length() >= checkAlt.length()
				? preseq.substring(preseq.length() - checkAlt.length()) : "";
			if (prevStr.equals(checkAlt)) {
				n.type = "dup";
				n.start -= checkAlt.length();
			} else {
				n.type = "ins";
				n.start--;
				n.end = n.start + 1;
			}
		}
		// Line 2056-2058: deletion
		else if (checkRef.length() >= 1 && checkAlt.equals("-")) {
			n.type = "del";
		}

		return n;
	}

	/**
	 * Simple HGVSg generation without reference sequence lookup.
	 * Used when reference genome is not available.
	 */
	private String generateSimple(String accession, int start, int end, String ref, String alt) {
		String notation;
		if ("-".equals(alt)) {
			notation = (start == end) ? start + "del" : start + "_" + end + "del";
		} else if ("-".equals(ref)) {
			notation = end + "_" + start + "ins" + alt;
		} else if (ref.length() == 1 && alt.length() == 1) {
			notation = start + ref + ">" + alt;
		} else {
			notation = (start == end) ? start + "delins" + alt : start + "_" + end + "delins" + alt;
		}
		return accession + ":g." + notation;
	}
}
