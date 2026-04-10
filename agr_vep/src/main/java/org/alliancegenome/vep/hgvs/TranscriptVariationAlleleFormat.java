package org.alliancegenome.vep.hgvs;

import org.alliancegenome.vep.annotation.TranscriptVariationAllele.CodingResult;
import org.alliancegenome.vep.bio.AminoAcid;

public class TranscriptVariationAlleleFormat {

	public String generate(String proteinId, CodingResult result) {
		int proteinPos = result.getHgvsProteinPosition();
		char refAA = result.getRefAA();
		char altAA = result.getAltAA();
		String consequence = result.getConsequence();

		if (proteinPos <= 0) return null;

		String prefix = proteinId != null ? proteinId + ":" : ":";

		// VEP _get_hgvs_peptides line 2075-2078: start_lost ALWAYS produces p.Met1?
		// regardless of other compound consequences (frameshift, stop_gained, etc.)
		// This check must come BEFORE the primary consequence switch.
		if (consequence.contains("start_lost")) {
			return prefix + "p.Met1?";
		}

		// VEP HGVSp uses the most severe consequence term for format selection.
		String primaryConsequence = consequence.contains("&") ? consequence.split("&")[0] : consequence;

		// If we have HGVS type from clip_alleles, use that for inframe notation
		String hgvsType = result.getHgvsType();

		switch (primaryConsequence) {
			case "synonymous_variant":
				return prefix + "p." + AminoAcid.threeLetterCode(refAA) + proteinPos + "%3D";

			case "missense_variant":
				return prefix + "p." + AminoAcid.threeLetterCode(refAA) + proteinPos + AminoAcid.threeLetterCode(altAA);

			case "stop_gained":
				return prefix + "p." + AminoAcid.threeLetterCode(refAA) + proteinPos + "Ter";

			case "stop_lost": {
				String extCount = result.getExtTerCount() != null ? result.getExtTerCount() : "?";
				return prefix + "p.Ter" + proteinPos + AminoAcid.threeLetterCode(altAA) + "extTer" + extCount;
			}

			case "start_lost":
				return prefix + "p.Met1?";

			case "stop_retained_variant":
				return prefix + "p.Ter" + proteinPos + "%3D";

			case "frameshift_variant": {
				String ref3 = AminoAcid.threeLetterCode(refAA);
				String alt3 = AminoAcid.threeLetterCode(altAA);
				if (altAA == '*') {
					// Stop gained immediately
					return prefix + "p." + ref3 + proteinPos + "Ter";
				}
				// VEP _get_fs_peptides line 2251-2254: frameshift at stop codon
				// where alt translation doesn't extend past → delextTer notation
				if (refAA == '*' && (altAA == 'X' || altAA == 0)) {
					String extCount = result.getExtTerCount() != null ? result.getExtTerCount() : "?";
					return prefix + "p.Ter" + proteinPos + "delextTer" + extCount;
				}
				String fsCount = result.getFsTerCount() != null ? result.getFsTerCount() : "?";
				return prefix + "p." + ref3 + proteinPos + alt3 + "fsTer" + fsCount;
			}

			case "inframe_insertion":
				return generateInframeInsertion(prefix, result, hgvsType);

			case "inframe_deletion":
				return generateInframeDeletion(prefix, result, hgvsType);

			case "protein_altering_variant":
				return generateProteinAltering(prefix, result, hgvsType);

			default:
				return null;
		}
	}

	private String generateInframeInsertion(String prefix, CodingResult result, String hgvsType) {
		if (hgvsType == null) return null;

		switch (hgvsType) {
			case "dup": {
				// VEP line 1859-1871: dup notation
				String clippedRef = result.getClippedRefPeptide();
				String clippedAlt = result.getClippedAltPeptide();
				// For dup, positions were adjusted to point at duplicated region
				int start = result.getHgvsProteinPosition();
				int end = result.getHgvsProteinEnd();
				if (clippedAlt != null && !clippedAlt.isEmpty()) {
					if (clippedAlt.length() == 1) {
						return prefix + "p." + AminoAcid.threeLetterCode(clippedAlt.charAt(0)) + start + "dup";
					} else {
						return prefix + "p." + AminoAcid.threeLetterCode(clippedAlt.charAt(0)) + start
							+ "_" + AminoAcid.threeLetterCode(clippedAlt.charAt(clippedAlt.length() - 1)) + end + "dup";
					}
				}
				return null;
			}
			case "ins": {
				// VEP line 1878-1912: flanking AAs + ins + inserted peptide
				char leftAA = result.getFlankLeftAA();
				char rightAA = result.getFlankRightAA();
				String altPep = result.getClippedAltPeptide();
				if (leftAA == 0 || rightAA == 0 || altPep == null || altPep.isEmpty()) return null;
				int start = result.getHgvsProteinPosition() - 1;
				int end = result.getHgvsProteinPosition();
				// Truncate alt after first stop
				int stopIdx = altPep.indexOf('*');
				if (stopIdx >= 0) altPep = altPep.substring(0, stopIdx + 1);
				return prefix + "p." + AminoAcid.threeLetterCode(leftAA) + start
					+ "_" + AminoAcid.threeLetterCode(rightAA) + end
					+ "ins" + toThreeLetterPeptide(altPep);
			}
			case "delins":
				return generateDelins(prefix, result);
			default:
				return null;
		}
	}

	private String generateInframeDeletion(String prefix, CodingResult result, String hgvsType) {
		if (hgvsType == null) return null;

		String clippedRef = result.getClippedRefPeptide();
		if (clippedRef == null || clippedRef.isEmpty()) return null;

		int start = result.getHgvsProteinPosition();
		int end = result.getHgvsProteinEnd();

		switch (hgvsType) {
			case "del": {
				// VEP line 1932-1942
				if (clippedRef.length() == 1) {
					return prefix + "p." + AminoAcid.threeLetterCode(clippedRef.charAt(0)) + start + "del";
				} else {
					return prefix + "p." + AminoAcid.threeLetterCode(clippedRef.charAt(0)) + start
						+ "_" + AminoAcid.threeLetterCode(clippedRef.charAt(clippedRef.length() - 1)) + end + "del";
				}
			}
			case "delins":
				return generateDelins(prefix, result);
			case ">": {
				// Single AA substitution after clipping
				char altAA = result.getClippedAltPeptide() != null && !result.getClippedAltPeptide().isEmpty()
					? result.getClippedAltPeptide().charAt(0) : result.getAltAA();
				return prefix + "p." + AminoAcid.threeLetterCode(clippedRef.charAt(0)) + start
					+ AminoAcid.threeLetterCode(altAA);
			}
			default:
				return null;
		}
	}

	private String generateProteinAltering(String prefix, CodingResult result, String hgvsType) {
		if (hgvsType == null) return null;

		switch (hgvsType) {
			case "del":
				return generateInframeDeletion(prefix, result, hgvsType);
			case "ins":
			case "dup":
				return generateInframeInsertion(prefix, result, hgvsType);
			case "delins":
				return generateDelins(prefix, result);
			case ">": {
				String clippedRef = result.getClippedRefPeptide();
				String clippedAlt = result.getClippedAltPeptide();
				if (clippedRef != null && clippedRef.length() == 1
						&& clippedAlt != null && clippedAlt.length() == 1) {
					return prefix + "p." + AminoAcid.threeLetterCode(clippedRef.charAt(0))
						+ result.getHgvsProteinPosition()
						+ AminoAcid.threeLetterCode(clippedAlt.charAt(0));
				}
				return generateDelins(prefix, result);
			}
			default:
				return null;
		}
	}

	private String generateDelins(String prefix, CodingResult result) {
		String clippedRef = result.getClippedRefPeptide();
		String clippedAlt = result.getClippedAltPeptide();
		if (clippedRef == null || clippedRef.isEmpty()) return null;
		if (clippedAlt == null || clippedAlt.isEmpty()) return null;

		int start = result.getHgvsProteinPosition();
		int end = result.getHgvsProteinEnd();

		if (clippedRef.length() == 1) {
			return prefix + "p." + AminoAcid.threeLetterCode(clippedRef.charAt(0)) + start
				+ "delins" + toThreeLetterPeptide(clippedAlt);
		} else {
			return prefix + "p." + AminoAcid.threeLetterCode(clippedRef.charAt(0)) + start
				+ "_" + AminoAcid.threeLetterCode(clippedRef.charAt(clippedRef.length() - 1)) + end
				+ "delins" + toThreeLetterPeptide(clippedAlt);
		}
	}

	private static String toThreeLetterPeptide(String oneLetterPep) {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < oneLetterPep.length(); i++) {
			sb.append(AminoAcid.threeLetterCode(oneLetterPep.charAt(i)));
		}
		return sb.toString();
	}
}
