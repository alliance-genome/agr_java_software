package org.alliancegenome.vep.hgvs;

import org.alliancegenome.vep.reference.ContigAccessionMap;
import org.alliancegenome.vep.reference.ReferenceGenome;

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

	public String generate(String chr, int start, int end, String ref, String alt) {
		String accession = contigMap.getAccession(chr);
		if (accession == null) return null;

		// VEP _clip_alleles (VariationFeature.pm line 2003-2061):
		// Trim common prefix then suffix from ref/alt for HGVS notation.
		// Only called when initial type is 'delins' (both non-empty).
		String clipRef = ref;
		String clipAlt = alt;
		int clipStart = start;
		int clipEnd = end;
		boolean wasClipped = false;
		if (!"-".equals(clipRef) && !"-".equals(clipAlt)) {
			wasClipped = true;
			// Prefix trim
			while (clipRef.length() > 0 && clipAlt.length() > 0
					&& clipRef.charAt(0) == clipAlt.charAt(0)) {
				clipRef = clipRef.substring(1);
				clipAlt = clipAlt.substring(1);
				clipStart++;
			}
			// Suffix trim
			while (clipRef.length() > 0 && clipAlt.length() > 0
					&& clipRef.charAt(clipRef.length() - 1) == clipAlt.charAt(clipAlt.length() - 1)) {
				clipRef = clipRef.substring(0, clipRef.length() - 1);
				clipAlt = clipAlt.substring(0, clipAlt.length() - 1);
				clipEnd--;
			}
			if (clipRef.isEmpty()) clipRef = "-";
			if (clipAlt.isEmpty()) clipAlt = "-";
		}

		// VEP _clip_alleles type determination (line 2028-2048):
		// After clipping, checks 1-char substitution BEFORE deletion.
		// "-" counts as 1 char, so clipped G/"" → G/"-" matches the ">" type.
		String notation;
		if ("-".equals(clipRef)) {
			// Insertion: check for duplication (VEP Sequence.pm line 570-588)
			if (isDuplication(chr, clipEnd, clipAlt)) {
				int dupLen = clipAlt.length();
				if (dupLen == 1) {
					notation = clipEnd + "dup";
				} else {
					notation = (clipEnd - dupLen + 1) + "_" + clipEnd + "dup";
				}
			} else {
				notation = clipEnd + "_" + clipStart + "ins" + clipAlt;
			}
		} else if (wasClipped && clipRef.length() == 1 && clipAlt.length() == 1
				&& !clipRef.equals(clipAlt)) {
			// VEP _clip_alleles line 2033-2038: after clipping, 1-char substitution.
			// "-" is 1 char, so clipped single-base deletions get ref>- notation.
			notation = clipStart + clipRef + ">" + clipAlt;
		} else if ("-".equals(clipAlt)) {
			// Deletion (pure deletion or multi-base from clipping)
			if (clipStart == clipEnd) {
				notation = clipStart + "del";
			} else {
				notation = clipStart + "_" + clipEnd + "del";
			}
		} else if (clipRef.length() == 1 && clipAlt.length() == 1) {
			// Regular SNP
			notation = clipStart + clipRef + ">" + clipAlt;
		} else {
			// Complex / delins
			if (clipStart == clipEnd) {
				notation = clipStart + "delins" + clipAlt;
			} else {
				notation = clipStart + "_" + clipEnd + "delins" + clipAlt;
			}
		}

		return accession + ":g." + notation;
	}

	/**
	 * VEP Sequence.pm line 570-588: check if inserted sequence matches
	 * the preceding reference bases (duplication detection).
	 */
	private boolean isDuplication(String chr, int precedingPos, String insertedSeq) {
		if (reference == null) return false;
		try {
			int dupLen = insertedSeq.length();
			int refStart = precedingPos - dupLen + 1;
			if (refStart < 1) return false;
			String precedingRef = reference.getSequence(chr, refStart, precedingPos);
			return insertedSeq.equalsIgnoreCase(precedingRef);
		} catch (Exception e) {
			return false;
		}
	}
}
