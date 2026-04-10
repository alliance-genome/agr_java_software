package org.alliancegenome.vep.annotation;

import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.vep.bio.CodonTable;
import org.alliancegenome.vep.bio.Sequence;
import org.alliancegenome.vep.model.CdsSegment;
import org.alliancegenome.vep.model.ExonModel;
import org.alliancegenome.vep.model.TranscriptModel;
import org.alliancegenome.vep.reference.ReferenceGenome;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class TranscriptVariationAllele {

	private final ReferenceGenome reference;

	public TranscriptVariationAllele(ReferenceGenome reference) {
		this.reference = reference;
	}

	public CodingResult annotate(TranscriptModel transcript, String chr, int variantStart, int variantEnd,
			String vepAllele, String refAllele) {

		try {
			return annotateInternal(transcript, chr, variantStart, variantEnd, vepAllele, refAllele);
		} catch (Exception e) {
			log.debug("Failed to annotate coding variant at {}:{} for {}: {}",
				chr, variantStart, transcript.getTranscriptId(), e.getMessage());
			return null;
		}
	}

	private CodingResult annotateInternal(TranscriptModel transcript, String chr, int variantStart, int variantEnd,
			String vepAllele, String refAllele) {

		// VEP seq_is_unambiguous_dna: allele must contain only A,C,G,T,-
		// Ambiguous bases (N, R, Y, etc.) produce X in peptide → coding_sequence_variant
		if (!isUnambiguousDna(vepAllele) || !isUnambiguousDna(refAllele)) {
			return null;
		}

		boolean isDeletion = "-".equals(vepAllele);
		boolean isInsertion = "-".equals(refAllele);

		// For indels, classify by frame
		if (isDeletion || isInsertion) {
			return annotateIndel(transcript, chr, variantStart, variantEnd, vepAllele, refAllele, isDeletion);
		}

		// Complex variant (different length ref/alt, neither is "-"):
		// VEP processes these the same as indels. The _get_alternate_cds builds
		// upstream[0..cds_start-2] + alt_allele + downstream[cds_end..]
		// replacing the full ref region with the alt allele.
		// vf_nt_len = cds_end - cds_start + 1, allele_len = length(alt)
		// frameshift if abs(allele_len - vf_nt_len) % 3 != 0
		if (vepAllele.length() != refAllele.length()) {
			// Determine if net effect is a deletion or insertion
			boolean netDeletion = vepAllele.length() < refAllele.length();
			return annotateIndel(transcript, chr, variantStart, variantEnd, vepAllele, refAllele, netDeletion);
		}

		// SNP in CDS
		if (vepAllele.length() == 1) {
			return annotateSNP(transcript, chr, variantStart, vepAllele);
		}

		// Multi-base substitution (MNV, equal length): treat as SNP at first position
		return annotateSNP(transcript, chr, variantStart, vepAllele.substring(0, 1));
	}

	private static boolean isUnambiguousDna(String seq) {
		for (int i = 0; i < seq.length(); i++) {
			char c = Character.toUpperCase(seq.charAt(i));
			if (c != 'A' && c != 'C' && c != 'G' && c != 'T' && c != '-') {
				return false;
			}
		}
		return true;
	}

	private CodingResult annotateSNP(TranscriptModel transcript, String chr, int pos, String altBase) {
		int cdsPos = genomicToCdsPosition(transcript, pos);
		if (cdsPos < 0) return null;

		int codonIndex = (cdsPos - 1) / 3;
		int posInCodon = (cdsPos - 1) % 3;

		String cdsSequence = buildCdsSequence(transcript, chr);
		if (cdsSequence == null || cdsPos > cdsSequence.length()) return null;

		int codonStart = codonIndex * 3;
		if (codonStart + 3 > cdsSequence.length()) return null;

		String refCodon = cdsSequence.substring(codonStart, codonStart + 3);
		char[] altCodonChars = refCodon.toCharArray();

		String effectiveAlt = transcript.isPositiveStrand() ? altBase : Sequence.reverseComplement(altBase);
		altCodonChars[posInCodon] = effectiveAlt.charAt(0);
		String altCodon = new String(altCodonChars);

		char refAA = CodonTable.translate(refCodon);
		char altAA = CodonTable.translate(altCodon);

		CodingResult result = new CodingResult();
		result.setCdsPosition(cdsPos);
		result.setProteinPosition(codonIndex + 1);
		result.setRefAA(refAA);
		result.setAltAA(altAA);
		result.setRefCodon(formatCodon(refCodon, posInCodon));
		result.setAltCodon(formatCodon(altCodon, posInCodon));
		result.setCdnaPosition(computeCdnaPosition(transcript, pos));

		// Classify
		if (cdsPos <= 3 && !transcript.isCdsStartNF() && CodonTable.isStart(refCodon) && !CodonTable.isStart(altCodon)) {
			result.setConsequence("start_lost");
		} else if (refAA == '*' && altAA == '*') {
			result.setConsequence("stop_retained_variant");
		} else if (refAA == '*' && altAA != '*') {
			result.setConsequence("stop_lost");
		} else if (altAA == '*') {
			result.setConsequence("stop_gained");
		} else if (refAA == altAA) {
			result.setConsequence("synonymous_variant");
		} else {
			result.setConsequence("missense_variant");
		}

		return result;
	}

	private CodingResult annotateIndel(TranscriptModel transcript, String chr, int variantStart, int variantEnd,
			String vepAllele, String refAllele, boolean isDeletion) {

		// VEP tracks both cds_start and cds_end (BaseTranscriptVariation.pm line 252-290).
		// genomic2cds returns coords in transcript order (5'→3').
		// cds_start = first.start (lower CDS value), cds_end = last.end (higher CDS value).
		// For minus strand: higher genomic → lower CDS (5' end).
		int cdsStart, cdsEnd;
		if (isDeletion) {
			int cdsA = genomicToCdsPosition(transcript, variantStart);
			int cdsB = genomicToCdsPosition(transcript, variantEnd);
			if (cdsA < 0 && cdsB < 0) return null;
			if (cdsA < 0) cdsA = cdsB;
			if (cdsB < 0) cdsB = cdsA;
			cdsStart = Math.min(cdsA, cdsB);
			cdsEnd = Math.max(cdsA, cdsB);
		} else {
			// Insertion: map both positions. VEP's cds_start from variantStart, cds_end from variantEnd.
			cdsStart = genomicToCdsPosition(transcript, variantStart);
			cdsEnd = genomicToCdsPosition(transcript, variantEnd);
			// If variantStart doesn't map (at CDS boundary), try variantEnd
			if (cdsStart < 0 && cdsEnd >= 0) {
				cdsStart = cdsEnd + 1;
			} else if (cdsEnd < 0 && cdsStart >= 0) {
				cdsEnd = cdsStart - 1;
			}
			if (cdsStart < 0 && cdsEnd < 0) return null;
		}

		String cdsSequence = buildCdsSequence(transcript, chr);
		if (cdsSequence == null) return null;

		// VEP: vf_nt_len = cds_end - cds_start + 1 (ref CDS span)
		// VEP: allele_len = length(alt allele) (0 for pure deletions)
		int vfNtLen; // ref CDS span
		int alleleLen; // alt allele length
		if (isDeletion) {
			vfNtLen = Math.abs(cdsEnd - cdsStart) + 1;
			if (vfNtLen <= 0) return null;
			alleleLen = "-".equals(vepAllele) ? 0 : vepAllele.length();
		} else {
			vfNtLen = "-".equals(refAllele) ? 0 : Math.abs(cdsEnd - cdsStart) + 1;
			alleleLen = vepAllele.length();
		}
		// indelLength = the ref span for deletions, alt length for insertions
		// Used for CDS position calculations and codon extraction
		int indelLength = isDeletion ? vfNtLen : alleleLen;

		// Use cdsStart for position (VEP uses cds_start for CDS_position output)
		int cdsPos = cdsStart;
		CodingResult result = new CodingResult();
		result.setCdsPosition(cdsPos);
		result.setCdsEnd(cdsEnd);
		result.setProteinPosition((cdsPos - 1) / 3 + 1);
		result.setCdnaPosition(computeCdnaPosition(transcript, variantStart));
		if (!isDeletion) {
			// Also store cdna for variantEnd for insertion range
			result.setCdnaEnd(computeCdnaPosition(transcript, variantEnd));
		}

		// VEP partial_codon guard (VariationEffect.pm line 1389-1414):
		// Checked BEFORE frameshift/inframe — blocks those if variant is in incomplete terminal codon.
		// VEP checks translation_start is defined (must map to CDS) and variant falls
		// entirely within the last incomplete codon. For deletions spanning past CDS end,
		// the fallback path handles stop_lost etc.
		if (isPartialCodon(transcript, cdsPos, cdsSequence.length())
				&& (!isDeletion || cdsEnd <= cdsSequence.length())) {
			result.setConsequence("incomplete_terminal_codon_variant");
			return result;
		}

		// VEP frameshift check (VariationEffect.pm line 1346-1387):
		// abs(allele_len - vf_nt_len) % 3 != 0
		boolean isFrameshift = Math.abs(alleleLen - vfNtLen) % 3 != 0;

		// Get the affected codon region peptides (like VEP's _get_peptide_alleles)
		int codonStart = ((cdsPos - 1) / 3) * 3;
		String refCodonRegion = safeSubstring(cdsSequence, codonStart, codonStart + 3);
		String refLocalPep = refCodonRegion != null ? String.valueOf(CodonTable.translate(refCodonRegion)) : null;

		// Check if variant overlaps stop codon (VEP: _overlaps_stop_codon, line 1358-1380)
		// Must check the full extent of the variant, not just the start position
		int cdsLen = cdsSequence.length();
		int stopCodonStart = cdsLen - 2; // 1-based: last 3 positions
		int varCdsStart = cdsPos;
		// Use CDS-relative length for CDS end position
		int varCdsEnd = isDeletion ? Math.max(cdsStart, cdsEnd) : cdsPos;
		boolean overlapsStop = varCdsEnd >= stopCodonStart || varCdsStart >= stopCodonStart;

		// Check if variant overlaps start codon (VEP: _overlaps_start_codon, line 965-986)
		// Start codon = CDS positions 1-3. VEP guards: return 0 if cds_start_NF (line 959)
		boolean overlapsStart = !transcript.isCdsStartNF() && (varCdsStart <= 3 || (isDeletion && varCdsStart <= 3));

		// Apply indel and get local alt peptide
		// VEP _get_alternate_cds appends 3'UTR so reading frame can extend into UTR for frameshifts
		String utr3 = build3PrimeUtr(transcript, chr);
		String altCdsWithUtr = applyIndelToCds(cdsSequence, cdsPos, vepAllele, refAllele, isDeletion, transcript, indelLength, utr3);
		// Also keep a CDS-only version for position-sensitive checks
		String altCds = applyIndelToCds(cdsSequence, cdsPos, vepAllele, refAllele, isDeletion, transcript, indelLength);

		// === VEP hgvs_protein() lines 1686-1741: exact method port ===

		// VEP codon() line 805, 818-820: translation positions
		// VEP: for insertions, tv_tr_start > tv_tr_end (mapper convention)
		// This makes codon_len = 0 for between-codon insertions
		int trStartCds = isDeletion ? cdsStart : Math.max(cdsStart, cdsEnd);
		int trEndCds = isDeletion ? cdsEnd : Math.min(cdsStart, cdsEnd);
		// VEP line 805: translation_start and translation_end
		// For insertions: start = ceil(cds_start/3), end = ceil(cds_end/3)
		// VEP cds_start is the HIGHER value for insertions → translationStart is higher
		int translationStart = (trStartCds - 1) / 3 + 1;
		int translationEnd = (trEndCds - 1) / 3 + 1;
		// VEP line 818-820: codon boundaries
		int codonCdsStart0 = (translationStart - 1) * 3; // = translationStart * 3 - 3 (0-based)
		int codonCdsEnd0 = translationEnd * 3 - 1;        // = translationEnd * 3 - 1 (0-based inclusive)
		// VEP line 820: codon_len = codon_cds_end - codon_cds_start + 1
		// For between-codon insertions: this can be 0 or negative
		int codonLen0 = codonCdsEnd0 - codonCdsStart0 + 1;
		// VEP line 828
		int altCodonLen0 = codonLen0 + (alleleLen - vfNtLen);

		if (refLocalPep != null && refLocalPep.length() > 0 && altCdsWithUtr != null) {
			// VEP codon() line 859: extract codon from ref and alt CDS
			String refCodonStr = vepCodon(cdsSequence, codonCdsStart0, codonLen0);
			String altCodonStr = vepCodon(altCds, codonCdsStart0, Math.max(0, altCodonLen0));

			// VEP peptide() line 684-778: translate codon to SHORT peptide
			String shortRefPep = vepPeptide(refCodonStr);
			String shortAltPep = vepPeptide(altCodonStr);

			// VEP line 1720-1726: guard — must have ref peptide, and ref != alt
			if (refLocalPep != null && refLocalPep.length() > 0 && altCdsWithUtr != null
					&& !shortRefPep.equals(shortAltPep)) {

				// VEP _clip_alleles (line 1725) on SHORT peptides
				HgvsNotation n = vepClipAlleles(shortRefPep, shortAltPep,
					translationStart, translationEnd);

				// VEP _get_hgvs_protein_type (line 1729)
				String hgvsType = vepGetHgvsProteinType(n, isFrameshift);

				// VEP _get_hgvs_peptides (line 1734)
				String fullRefPep = translateCds(safeSubstring(cdsSequence, 0, cdsSequence.length()));

				// Line 2033-2037: for frameshifts, walk full alt translation
				if ("fs".equals(hgvsType)) {
					HgvsNotation fsResult = vepGetFsPeptides(n, cdsSequence, altCds, translationStart);
					if (fsResult == null) {
						// _get_fs_peptides returned undef — skip HGVSp
					} else if ("del".equals(fsResult.type) || "=".equals(fsResult.type)) {
						hgvsType = fsResult.type;
					}
					// fsResult updates n.start, n.ref, n.alt in place
				}

				String noStop = null;
				if (fullRefPep != null) {
					noStop = fullRefPep.endsWith("*")
						? fullRefPep.substring(0, fullRefPep.length() - 1) : fullRefPep;
				}

				// VEP _get_hgvs_peptides line 2038-2061: "ins" type
				if ("ins".equals(hgvsType) && noStop != null) {
					// Line 2041: _check_peptides_post_var → _shift_3prime
					vepShift3Prime(n, "ins", noStop);

					// Line 2044: _check_for_peptide_duplication
					if (!n.alt.contains("*")) {
						vepCheckForPeptideDuplication(n, noStop);
					}
					if ("dup".equals(n.type)) hgvsType = "dup";

					// Line 2047-2060: set ref to surrounding peptides for ins notation
					if ("ins".equals(hgvsType) && noStop != null) {
						int minPos = Math.min(n.start, n.end);
						if (minPos >= 1 && minPos + 1 <= noStop.length()) {
							// _get_surrounding_peptides(min, original_ref, 2) → 2 chars from min
							String surr = noStop.substring(minPos - 1, Math.min(minPos + 1, noStop.length()));
							if (surr.length() == 2) {
								n.ref = surr;
							}
						}
					}
				}
				// VEP _get_hgvs_peptides line 2062-2064: "del" type
				else if ("del".equals(hgvsType) && noStop != null) {
					// Line 2064: _check_peptides_post_var for deletions too
					vepShift3Prime(n, "del", noStop);
				}

				// VEP _get_hgvs_peptides line 2072: alt="-" → alt="del"
				if ("-".equals(n.alt)) n.alt = "del";

				// VEP _get_hgvs_peptides line 2075-2078: start_lost overrides
				if (result.getConsequence() != null && result.getConsequence().contains("start_lost")) {
					n.alt = "?";
					n.type = "";
				}

				// Set HGVSp results from notation
				result.setHgvsProteinPosition(n.start);
				result.setHgvsProteinEnd(n.end);
				result.setClippedRefPeptide(n.ref);
				result.setClippedAltPeptide(n.alt);
				result.setHgvsType(hgvsType);

				// ref/alt AA at first differing position
				if ("fs".equals(hgvsType) && n.ref != null && n.ref.length() == 1
						&& n.alt != null && n.alt.length() == 1) {
					// _get_fs_peptides set ref/alt to the first differing AA
					result.setRefAA(n.ref.charAt(0));
					result.setAltAA(n.alt.charAt(0));
				} else {
					int prefixLen = n.preseq != null ? n.preseq.length() : 0;
					if (prefixLen < shortRefPep.length()) {
						result.setRefAA(shortRefPep.charAt(prefixLen));
					} else if (!shortRefPep.isEmpty()) {
						result.setRefAA(shortRefPep.charAt(0));
					}
					if (prefixLen < shortAltPep.length()) {
						result.setAltAA(shortAltPep.charAt(prefixLen));
					}
				}

				// Flanking AAs for insertion HGVSp (VEP _get_surrounding_peptides)
				if ("ins".equals(hgvsType) || "dup".equals(hgvsType)) {
					if (fullRefPep != null) {
						int insProtPos = "dup".equals(hgvsType) ? n.end + 1 : n.start;
						if (insProtPos >= 2 && insProtPos <= fullRefPep.length()) {
							result.setFlankLeftAA(fullRefPep.charAt(insProtPos - 2));
							result.setFlankRightAA(fullRefPep.charAt(insProtPos - 1));
						}
					}
				}
				// Store notation + CDS sequences for _get_hgvs_protein_format
				n.type = hgvsType;
				result.setHgvsNotation(n);
				result.setCdsSequence(cdsSequence);
				result.setAltCdsSequence(altCds);
			}
		}

		// VEP _ins_del_stop_altered (line 1423): checks the codon at the ORIGINAL stop position
		// in the modified CDS: substr($utr_and_translateable, length($translateable) - 3, 3)
		int stopIdx0 = cdsLen - 3; // 0-based index of stop codon start
		String refStopCodon = safeSubstring(cdsSequence, stopIdx0, stopIdx0 + 3);
		boolean refHasStop = refStopCodon != null && CodonTable.isStop(refStopCodon);

		// In alt CDS, check the same position (original CDS length - 3)
		String altStopRegion = altCds != null ? safeSubstring(altCds, stopIdx0, stopIdx0 + 3) : null;
		boolean altHasStopAtSamePos = altStopRegion != null && altStopRegion.length() == 3 && CodonTable.isStop(altStopRegion);

		List<String> consequences = new ArrayList<>();

		if (isFrameshift) {
			consequences.add("frameshift_variant");

			if (overlapsStop && refHasStop && !altHasStopAtSamePos) {
				consequences.add("stop_lost");
			}
			if (overlapsStart) {
				// VEP has TWO independent paths for start codon consequences:
				//
				// 1. _ins_del_start_altered (line 998-1014): checks if ATG codon is
				//	  physically changed by the indel. If NOT altered → start_retained_variant.
				//	  If altered → start_lost (line 862).
				//
				// 2. Peptide check (line 864-873): checks if the translated protein differs.
				//	  For frameshifts, the protein always differs → start_lost fires via this path
				//	  EVEN WHEN _ins_del_start_altered is false (ATG preserved).
				//
				// Result: frameshift at start codon with preserved ATG produces BOTH
				// start_lost (peptide changed) AND start_retained_variant (ATG preserved).
				boolean startAltered = true;
				if (altCds != null && cdsSequence != null) {
					if (altCds.length() >= cdsSequence.length()) {
						String tail = altCds.substring(altCds.length() - cdsSequence.length());
						startAltered = !tail.equals(cdsSequence);
					}
				}
				if (startAltered) {
					consequences.add("start_lost");
				} else {
					// ATG preserved — start_retained fires
					consequences.add("start_retained_variant");
					// But for frameshifts, VEP's peptide check also fires start_lost
					// because the protein IS different (reading frame shifted)
					consequences.add("start_lost");
				}
			}
			if (overlapsStop && refHasStop && altHasStopAtSamePos) {
				consequences.add("stop_retained_variant");
			}

			// VEP stop_gained (VariationEffect.pm line 1146-1166):
			// Checks _get_peptide_alleles: alt_pep =~ /\*/ and ref_pep !~ /\*/
			// For frameshifts, translates the "codon" region from the modified CDS.
			// The codon region starts at the affected codon and has length = codonLen + (indelDiff)
			// This checks if the frameshift introduces a premature stop codon.
			if (altCds != null && !consequences.contains("stop_lost")) {
				int codonStart0 = ((cdsPos - 1) / 3) * 3; // 0-based
				int protStart = codonStart0 / 3 + 1;
				int protEnd = protStart;
				if (isDeletion) {
					int cdsEndPos = cdsPos + indelLength - 1;
					protEnd = (cdsEndPos - 1) / 3 + 1;
				}
				int clipCodonS0 = (protStart - 1) * 3;
				int clipCodonE0 = protEnd * 3 - 1;
				int codonLen = clipCodonE0 - clipCodonS0 + 1;
				int diff = isDeletion ? -indelLength : indelLength;
				int altRegionLen = codonLen + diff;

				if (altRegionLen > 0) {
					String refRegion = safeSubstring(cdsSequence, clipCodonS0, clipCodonS0 + codonLen);
					String altRegion = safeSubstring(altCds, clipCodonS0, clipCodonS0 + altRegionLen);

					if (refRegion != null && altRegion != null) {
						String refPep = translateCds(refRegion);
						String altPep = translateCds(altRegion);
						if (altPep.contains("*") && !refPep.contains("*")) {
							consequences.add("stop_gained");
						}
					}
				}
			}
		} else {
			// In-frame indel
			// Get local codon alleles matching VEP's _get_codon_alleles logic
			// (TranscriptVariationAllele.pm line 841-877)
			int protStart = (cdsPos - 1) / 3 + 1;
			int protEnd = protStart;
			if (isDeletion) {
				protEnd = (Math.max(cdsStart, cdsEnd) - 1) / 3 + 1;
			}
			int codonCdsStart = protStart * 3 - 2;
			int codonCdsEnd = protEnd * 3;
			int codonLen = codonCdsEnd - codonCdsStart + 1;

			String refCodon = safeSubstring(cdsSequence, codonCdsStart - 1, codonCdsStart - 1 + codonLen);
			int altCodonLen = codonLen + (isDeletion ? -indelLength : indelLength);
			String altCodon = altCds != null ? safeSubstring(altCds, codonCdsStart - 1,
				codonCdsStart - 1 + Math.max(0, altCodonLen)) : null;

			String refPep = refCodon != null ? translateCds(refCodon) : null;
			String altPep = altCodon != null ? translateCds(altCodon) : null;

			// Trim alt_pep after first stop (VEP inframe_insertion line 1124)
			String altPepTrimmed = altPep;
			if (altPepTrimmed != null) {
				int stopIdx = altPepTrimmed.indexOf('*');
				if (stopIdx >= 0 && stopIdx < altPepTrimmed.length() - 1) {
					altPepTrimmed = altPepTrimmed.substring(0, stopIdx + 1);
				}
			}

			// VEP checks predicates independently. For deletions:
			// 1. stop_lost: overlaps stop codon AND ref has stop AND alt doesn't
			// 2. start_lost: overlaps start codon
			// 3. inframe_deletion: check codon sequences (line 1175)
			// 4. stop_gained: local ref_pep has no stop AND local alt_pep has stop (line 1224)
			// 5. protein_altering: catches remaining in-frame that don't match simple patterns
			if (isDeletion) {
				// First determine the base consequence
				// VEP stop_lost: alt peptide doesn't contain '*' AND ref does.
				// When altPep is null/empty (deletion removes entire codon region),
				// the stop codon is also lost — treat as stop_lost.
				boolean altHasNoStop = altPep == null || altPep.isEmpty() || !altPep.contains("*");
				if (overlapsStop && refHasStop && altHasNoStop) {
					consequences.add("stop_lost");
					consequences.add("inframe_deletion");
				} else if (overlapsStart) {
					// VEP start_lost line 862: for inframe deletions, the
					// _ins_del_start_altered path is BLOCKED (!(inframe_deletion) = false).
					// start_lost fires via peptide check (line 864-873).
					// start_retained fires when _ins_del_start_altered is false.
					boolean startAltered = true;
					if (altCds != null && cdsSequence != null && altCds.length() >= cdsSequence.length()) {
						String tail = altCds.substring(altCds.length() - cdsSequence.length());
						startAltered = !tail.equals(cdsSequence);
					}
					boolean pepRetainsStart = false;
					if (refPep != null && altPep != null) {
						String altPepTr = altPep;
						int si = altPepTr.indexOf('*');
						if (si >= 0 && si < altPepTr.length() - 1) altPepTr = altPepTr.substring(0, si + 1);
						pepRetainsStart = altPepTr.startsWith(refPep) || altPepTr.endsWith(refPep);
					}
					if (!pepRetainsStart) {
						consequences.add("start_lost");
					}
					if (!startAltered) {
						consequences.add("start_retained_variant");
					}
					consequences.add("inframe_deletion");
				} else {
					// Check inframe_deletion vs protein_altering (VEP line 1167-1182)
					boolean isInframeDel = false;
					if (refCodon != null && altCodon != null) {
						if (refCodon.startsWith(altCodon) || refCodon.endsWith(altCodon)) {
							isInframeDel = true;
						} else {
							String[] trimmed = trimSequences(refCodon, altCodon);
							if (trimmed[1].isEmpty() && trimmed[0].length() % 3 == 0) {
								isInframeDel = true;
							}
						}
					}
					if (isInframeDel) {
						consequences.add("inframe_deletion");
					} else if (refPep != null && altPep != null
						&& refPep.length() != altPep.length()
						&& !refPep.startsWith("*") && !altPep.startsWith("*")) {
						consequences.add("protein_altering_variant");
					} else {
						consequences.add("inframe_deletion");
					}

					// VEP stop_gained (VariationEffect.pm line 1146-1166):
					// Checks if alt peptide contains '*' and ref peptide doesn't
					if (altPep != null && refPep != null
						&& altPep.contains("*") && !refPep.contains("*")) {
						consequences.add("stop_gained");
					}
				}
			} else {
				// Insertion
				if (overlapsStart) {
					// VEP start_lost line 862: for inframe insertions, the
					// _ins_del_start_altered path is BLOCKED (!(inframe_insertion) = false).
					// start_lost can only fire via peptide check (line 864-873):
					// alt_pep must NOT start or end with ref_pep.
					// start_retained_variant fires when _ins_del_start_altered is false.
					boolean startAltered = true;
					if (altCds != null && cdsSequence != null && altCds.length() >= cdsSequence.length()) {
						String tail = altCds.substring(altCds.length() - cdsSequence.length());
						startAltered = !tail.equals(cdsSequence);
					}

					// VEP peptide check for start_lost (line 869-873)
					boolean pepRetainsStart = false;
					if (refPep != null && altPepTrimmed != null) {
						pepRetainsStart = altPepTrimmed.startsWith(refPep) || altPepTrimmed.endsWith(refPep);
					}

					if (!pepRetainsStart) {
						consequences.add("start_lost");
					}
					if (!startAltered) {
						consequences.add("start_retained_variant");
					}
					if (consequences.isEmpty()) {
						consequences.add("inframe_insertion");
					}
				} else {
					// VEP inframe_insertion: check PEPTIDES (line 1113-1126)
					// VEP uses $bvfoa->peptide and _get_ref_pep which return the
					// translated codons at the variant position
					// For a 3bp insertion: ref = 1 AA, alt = 2 AAs (the codon gets extended)
					// The check is: alt_pep starts or ends with ref_pep
					//
					// Default to inframe_insertion, then check if protein_altering applies
					boolean isProteinAltering = false;

					if (refPep != null && altPepTrimmed != null && refPep.length() > 0) {
						// VEP trims stops: $alt_pep =~ s/\*.+/\*/
						// Then checks: ($alt_pep =~ /^\Q$ref_pep\E/) || ($alt_pep =~ /\Q$ref_pep\E$/)
						boolean pepMatch = altPepTrimmed.startsWith(refPep) || altPepTrimmed.endsWith(refPep);

						if (!pepMatch && refPep.length() != altPep.length()
							&& !refPep.startsWith("*") && !altPep.startsWith("*")) {
							isProteinAltering = true;
							log.debug("protein_altering: cdsPos={} refCodon=[{}] altCodon=[{}] refPep=[{}] altPep=[{}] altPepTrimmed=[{}]",
								cdsPos, refCodon, altCodon, refPep, altPep, altPepTrimmed);
						}
					}

					if (isProteinAltering) {
						consequences.add("protein_altering_variant");
					} else {
						consequences.add("inframe_insertion");
						if (overlapsStop && refHasStop && altPep != null && altPep.contains("*")) {
							consequences.add("stop_retained_variant");
						}
					}
					// VEP stop_gained (line 1162): alt_pep contains '*' anywhere AND ref_pep doesn't.
					// Evaluated independently — can coexist with protein_altering or inframe_insertion.
					if (altPep != null && altPep.contains("*") && (refPep == null || !refPep.contains("*"))) {
						consequences.add("stop_gained");
					}
				}
			}
		}

		// Populate amino acids and codons for indels matching VEP's model exactly.
		// VEP codon() (TranscriptVariationAllele.pm line 790-868):
		//	 codon_len = codon_cds_end - codon_cds_start + 1
		//	 For insertions between codons: cds_start > cds_end → codon_len = 0 → codon = '-', peptide = '-'
		// VEP display_codon (line 884): all lowercase, uppercase variant bases
		//	 Alt of deletion / ref of insertion: feature_seq = '-' → all lowercase
		// VEP pep_allele_string (line 610): ref_pep/alt_pep
		{
			// Reuse translationStart/End, codonCdsStart0/End0, codonLen0, altCodonLen0
			// computed earlier for SHORT peptide clipping (same formulas).

			String rc, ac, rp, ap;
			if (codonLen0 <= 0) {
				// Between-codon insertion: VEP alt TVA sets codon='-', peptide='-' (line 861-863)
				rc = "-";
				rp = "-";
				// Alt codon from alt CDS
				ac = altCds != null ? safeSubstring(altCds, codonCdsStart0, codonCdsStart0 + Math.max(0, altCodonLen0)) : null;
				ap = ac != null && ac.length() > 0 ? translateCds(ac) : "-";
			} else {
				rc = safeSubstring(cdsSequence, codonCdsStart0, codonCdsStart0 + codonLen0);
				ac = altCds != null ? safeSubstring(altCds, codonCdsStart0, codonCdsStart0 + Math.max(0, altCodonLen0)) : null;
				rp = rc != null ? translateCds(rc) : null;
				ap = ac != null && ac.length() > 0 ? translateCds(ac) : "-";
			}

			// VEP pep_allele_string (line 610-622)
			if (rp != null) {
				String rpStr = rp.isEmpty() ? "-" : rp;
				String apStr = (ap == null || ap.isEmpty()) ? "-" : ap;
				result.setAminoAcids(pepAlleleString(rpStr, apStr));
			}

			// VEP display_codon (line 884-915) + display_codon_allele_string (line 658-673)
			// Exact Perl port: codon_position is 1-based (TranscriptVariation.pm line 302)
			int codonPosition1 = ((trStartCds - 1) % 3) + 1;
			// VEP feature_seq: ref TVA gets refAllele, alt TVA gets vepAllele
			// For deletions: ref feature_seq = deleted bases, alt feature_seq = "-"
			// For insertions: ref feature_seq = "-", alt feature_seq = inserted bases
			String refFeatureSeq = "-".equals(refAllele) ? "-" : refAllele;
			String altFeatureSeq = "-".equals(vepAllele) ? "-" : vepAllele;
			String refDisplay = displayCodon(rc, refFeatureSeq, codonPosition1);
			String altDisplay = displayCodon(ac, altFeatureSeq, codonPosition1);
			if (refDisplay == null) refDisplay = "-";
			if (altDisplay == null) altDisplay = "-";
			result.setCodons(displayCodonAlleleString(refDisplay, altDisplay));
		}

		// Sort by VEP rank (most severe first) to match VEP output order
		consequences.sort((a, b) -> Integer.compare(
			ConsequenceSeverity.getRank(a), ConsequenceSeverity.getRank(b)));
		result.setConsequence(String.join("&", consequences));

		// Compute fsTer/extTer count (VEP _stop_loss_extra_AA, line 2386-2435)
		if (altCdsWithUtr != null) {
			if (consequences.contains("frameshift_variant") && !consequences.contains("stop_gained")) {
				result.setFsTerCount(computeTerCount(altCdsWithUtr, result.getHgvsProteinPosition(), cdsSequence.length()));
			}
			if (consequences.contains("stop_lost")) {
				int origStopProtPos = cdsSequence.length() / 3;
				result.setExtTerCount(computeExtTerCount(altCdsWithUtr, origStopProtPos));
			}
		}

		return result;
	}

	/**
	 * VEP _stop_loss_extra_AA for frameshifts (line 2415-2418):
	 * Translates alt CDS+UTR from variant position, finds first stop, returns count.
	 */
	private String computeTerCount(String altCdsWithUtr, int fromProtPos, int origCdsLength) {
		// Translate from the variant protein position to end
		int startBase0 = (fromProtPos - 1) * 3;
		if (startBase0 < 0) startBase0 = 0;
		String region = safeSubstring(altCdsWithUtr, startBase0, altCdsWithUtr.length());
		if (region == null || region.isEmpty()) return "?";
		String pep = translateCds(region);
		int stopIdx = pep.indexOf('*');
		if (stopIdx >= 0) {
			return String.valueOf(stopIdx + 1);
		}
		return "?";
	}

	/**
	 * VEP _stop_loss_extra_AA for stop_lost (non-frameshift, line 2405-2412):
	 * Count = position_of_new_stop - original_stop_position
	 */
	private String computeExtTerCount(String altCdsWithUtr, int origStopProtPos) {
		// Translate from just past the original stop position
		int startBase0 = (origStopProtPos - 1) * 3;
		if (startBase0 < 0) startBase0 = 0;
		String region = safeSubstring(altCdsWithUtr, startBase0, altCdsWithUtr.length());
		if (region == null || region.isEmpty()) return "?";
		String pep = translateCds(region);
		int stopIdx = pep.indexOf('*');
		if (stopIdx >= 0) {
			return String.valueOf(stopIdx + 1);
		}
		return "?";
	}

	/**
	 * Trim common prefix and suffix from two sequences.
	 * Matches VEP's Bio::EnsEMBL::Variation::Utils::Sequence::trim_sequences
	 */
	private String[] trimSequences(String ref, String alt) {
		int prefixLen = 0;
		int minLen = Math.min(ref.length(), alt.length());
		while (prefixLen < minLen && ref.charAt(prefixLen) == alt.charAt(prefixLen)) {
			prefixLen++;
		}
		ref = ref.substring(prefixLen);
		alt = alt.substring(prefixLen);

		int suffixLen = 0;
		minLen = Math.min(ref.length(), alt.length());
		while (suffixLen < minLen && ref.charAt(ref.length() - 1 - suffixLen) == alt.charAt(alt.length() - 1 - suffixLen)) {
			suffixLen++;
		}
		if (suffixLen > 0) {
			ref = ref.substring(0, ref.length() - suffixLen);
			alt = alt.substring(0, alt.length() - suffixLen);
		}
		return new String[]{ref, alt};
	}

	private String formatDisplayCodon(String codon, int codonPos, int variantLen) {
		StringBuilder sb = new StringBuilder(codon.toLowerCase());
		int end = Math.min(codonPos + variantLen, sb.length());
		for (int i = codonPos; i < end; i++) {
			sb.setCharAt(i, Character.toUpperCase(sb.charAt(i)));
		}
		return sb.toString();
	}

	private String safeSubstring(String s, int start, int end) {
		if (s == null || start < 0 || start >= s.length()) return null;
		return s.substring(start, Math.min(end, s.length()));
	}

	private String translateCds(String cds) {
		StringBuilder pep = new StringBuilder();
		int wholeLen = (cds.length() / 3) * 3;
		for (int i = 0; i < wholeLen; i += 3) {
			char aa = CodonTable.translate(cds.substring(i, i + 3));
			pep.append(aa);
			// VEP peptide() does NOT break at stop — translates full codon region
		}
		// VEP peptide() line 766-768: partial trailing codon → append 'X'
		if (cds.length() % 3 != 0 && (pep.length() == 0 || pep.charAt(pep.length() - 1) != '*')) {
			pep.append('X');
		}
		return pep.toString();
	}

	/**
	 * BioPerl translate() — only translates COMPLETE codons, ignores partial trailing codons.
	 * Used by VEP's _get_fs_peptides and _stop_loss_extra_AA which call
	 * $alt_cds->translate()->seq() (BioPerl, not VEP's peptide() method).
	 */
	private String translateCdsWholeOnly(String cds) {
		if (cds == null) return "";
		StringBuilder pep = new StringBuilder();
		int wholeLen = (cds.length() / 3) * 3;
		for (int i = 0; i < wholeLen; i += 3) {
			char aa = CodonTable.translate(cds.substring(i, i + 3));
			pep.append(aa);
		}
		return pep.toString();
	}

	/** Count the number of CDS bases within a genomic range.
	 * VEP's coordinate mapper splices out introns; this is the equivalent. */
	private int computeCdsDeletionLength(TranscriptModel transcript, int genomicStart, int genomicEnd) {
		int total = 0;
		for (CdsSegment seg : transcript.getCdsSegments()) {
			int ovStart = Math.max(genomicStart, seg.getStart());
			int ovEnd = Math.min(genomicEnd, seg.getEnd());
			if (ovStart <= ovEnd) {
				total += ovEnd - ovStart + 1;
			}
		}
		return total;
	}

	/**
	 * VEP _ins_del_stop_altered (VariationEffect.pm line 1292-1344):
	 * Checks if a deletion alters the stop codon. Used as fallback when
	 * cds_end is undef (variant extends into UTR/intron).
	 */
	/**
	 * VEP _ins_del_stop_altered (VariationEffect.pm line 1292-1344):
	 * Checks if a deletion alters the stop codon. Builds CDS+3'UTR sequence,
	 * applies the edit, and checks if the codon at the original stop position
	 * is still a stop codon.
	 */
	/**
	 * VEP _ins_del_stop_altered (VariationEffect.pm line 1292-1344):
	 * Checks if a deletion alters the stop codon by building CDS+3'UTR,
	 * applying the edit, and checking if the codon at the original stop
	 * position is still a stop codon.
	 *
	 * VEP guards (line 1312): return 0 unless $cdna_start && $cdna_end && $cds_start
	 * - cdna_start/end = both endpoints must be in exons (genomic2cdna gives Coordinate, not Gap)
	 * - cds_start = the FIRST CDS base in the mapped range (not the variant start)
	 *
	 * VEP edit (line 1325): substr($cds_and_utr, $cds_start - 1, cdna_span) = alt_seq
	 * - cds_start = CDS position of first CDS base in range
	 * - cdna_span = cdna_end - cdna_start + 1 (full exonic span including UTR)
	 */
	public boolean isStopAltered(TranscriptModel transcript, String chr, int variantStart, int variantEnd) {
		try {
			// 1. Check overlap with stop codon (genomic)
			int stopLow, stopHigh;
			if (transcript.isPositiveStrand()) {
				stopHigh = transcript.getCdsEnd();
				stopLow = stopHigh - 2;
			} else {
				stopLow = transcript.getCdsStart();
				stopHigh = stopLow + 2;
			}
			if (variantStart > stopHigh || variantEnd < stopLow) return false;

			// 2. Build CDS and verify stop codon
			String cds = buildCdsSequence(transcript, chr);
			if (cds == null || cds.length() < 3) return false;
			if (!CodonTable.isStop(cds.substring(cds.length() - 3))) return false;

			// 3. Build 3' UTR sequence
			String utr3 = build3PrimeUtr(transcript, chr);
			String cdsAndUtr = cds + utr3;

			// 4. Find the FIRST CDS position in the variant range
			// VEP uses genomic2cds which maps the range and takes the first Coordinate.
			// The variant start may be in UTR or intron, but the range still overlaps CDS.
			int cdsPos = findFirstCdsPositionInRange(transcript, variantStart, variantEnd);
			if (cdsPos < 0) return false;

			// 5. Compute exonic edit length (= cDNA span, VEP line 1324)
			int editLen = 0;
			for (ExonModel exon : transcript.getExons()) {
				int ovStart = Math.max(variantStart, exon.getStart());
				int ovEnd = Math.min(variantEnd, exon.getEnd());
				if (ovStart <= ovEnd) {
					editLen += ovEnd - ovStart + 1;
				}
			}
			if (editLen <= 0) return false;

			// 6. Apply edit to CDS+UTR (VEP line 1325)
			int delStart = cdsPos - 1;
			if (delStart < 0) delStart = 0;
			if (delStart + editLen > cdsAndUtr.length()) editLen = cdsAndUtr.length() - delStart;
			if (editLen <= 0) return false;
			String modified = cdsAndUtr.substring(0, delStart) + cdsAndUtr.substring(delStart + editLen);

			// 7. VEP line 1328: if shorter than translateable → altered
			if (modified.length() < cds.length()) return true;

			// 8. VEP line 1332-1340: check codon at original stop position
			int stopIdx = cds.length() - 3;
			if (stopIdx + 3 > modified.length()) return true;
			String newStop = modified.substring(stopIdx, stopIdx + 3);
			return !CodonTable.isStop(newStop);
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * Find the CDS position of the first CDS base within a genomic range.
	 * VEP's genomic2cds maps the entire range and takes the first Coordinate.
	 * The variant start may be in UTR/intron but the range still overlaps CDS.
	 */
	private int findFirstCdsPositionInRange(TranscriptModel transcript, int variantStart, int variantEnd) {
		// First try the exact start position
		int pos = genomicToCdsPosition(transcript, variantStart);
		if (pos >= 0) return pos;

		// Start not in CDS — find the first CDS segment that overlaps the range
		List<CdsSegment> segments = transcript.getCdsSegments();
		if (transcript.isPositiveStrand()) {
			for (CdsSegment seg : segments) {
				if (seg.getEnd() >= variantStart && seg.getStart() <= variantEnd) {
					int firstBase = Math.max(seg.getStart(), variantStart);
					return genomicToCdsPosition(transcript, firstBase);
				}
			}
		} else {
			for (int i = segments.size() - 1; i >= 0; i--) {
				CdsSegment seg = segments.get(i);
				if (seg.getEnd() >= variantStart && seg.getStart() <= variantEnd) {
					int firstBase = Math.min(seg.getEnd(), variantEnd);
					return genomicToCdsPosition(transcript, firstBase);
				}
			}
		}
		return -1;
	}

	/**
	 * VEP _ins_del_start_altered (VariationEffect.pm line 976-1015):
	 * Checks if a deletion alters the start codon by building 5'UTR+CDS,
	 * applying the edit, and checking if the CDS portion is preserved.
	 */
	public boolean isStartAltered(TranscriptModel transcript, String chr, int variantStart, int variantEnd) {
		try {
			// 1. Check overlap with start codon (genomic)
			int startLow, startHigh;
			if (transcript.isPositiveStrand()) {
				startLow = transcript.getCdsStart();
				startHigh = startLow + 2;
			} else {
				startHigh = transcript.getCdsEnd();
				startLow = startHigh - 2;
			}
			if (variantStart > startHigh || variantEnd < startLow) return false;

			// 2. Build CDS and verify start codon
			String cds = buildCdsSequence(transcript, chr);
			if (cds == null || cds.length() < 3) return false;

			// 3. Build 5' UTR sequence and concatenate
			String utr5 = build5PrimeUtr(transcript, chr);
			String utrAndCds = utr5 + cds;

			// 4. Compute cDNA position of variant start (exonic offset from 5' end)
			int cdnaStart = computeCdnaPosition(transcript, variantStart);
			if (cdnaStart < 0) {
				// Variant start might be in intron — find first exonic position in range
				for (ExonModel exon : transcript.getExons()) {
					int firstExonic = transcript.isPositiveStrand()
						? Math.max(variantStart, exon.getStart())
						: Math.min(variantEnd, exon.getEnd());
					if (firstExonic >= exon.getStart() && firstExonic <= exon.getEnd()) {
						cdnaStart = computeCdnaPosition(transcript, firstExonic);
						if (cdnaStart >= 0) break;
					}
				}
			}
			if (cdnaStart < 0) return false;

			// 5. Compute exonic edit length
			int editLen = 0;
			for (ExonModel exon : transcript.getExons()) {
				int ovStart = Math.max(variantStart, exon.getStart());
				int ovEnd = Math.min(variantEnd, exon.getEnd());
				if (ovStart <= ovEnd) {
					editLen += ovEnd - ovStart + 1;
				}
			}
			if (editLen <= 0) return false;

			// 6. Apply edit to 5'UTR+CDS (VEP line 1006)
			int delStart = cdnaStart - 1;
			if (delStart < 0) delStart = 0;
			if (delStart + editLen > utrAndCds.length()) editLen = utrAndCds.length() - delStart;
			if (editLen <= 0) return false;
			String modified = utrAndCds.substring(0, delStart) + utrAndCds.substring(delStart + editLen);

			// 7. VEP line 1009: if shorter than CDS → altered
			if (modified.length() < cds.length()) return true;

			// 8. VEP line 1011: check if CDS portion is preserved at the END of modified
			String tail = modified.substring(modified.length() - cds.length());
			return !tail.equals(cds);
		} catch (Exception e) {
			return false;
		}
	}

	/** Build the 5' UTR sequence from FASTA for this transcript. */
	private String build5PrimeUtr(TranscriptModel transcript, String chr) {
		StringBuilder utr = new StringBuilder();
		int cdsEnd = transcript.getCdsEnd();
		int cdsStart = transcript.getCdsStart();

		if (transcript.isPositiveStrand()) {
			// 5'UTR: exonic regions before CDS start
			for (ExonModel exon : transcript.getExons()) {
				if (exon.getStart() >= cdsStart) break;
				int utrEnd = Math.min(exon.getEnd(), cdsStart - 1);
				String seq = reference.getSequence(chr, exon.getStart(), utrEnd);
				utr.append(seq.toUpperCase());
			}
		} else {
			// 5'UTR on - strand: exonic regions after CDS end (reverse complement)
			for (int i = transcript.getExons().size() - 1; i >= 0; i--) {
				ExonModel exon = transcript.getExons().get(i);
				if (exon.getEnd() <= cdsEnd) break;
				int utrStart = Math.max(exon.getStart(), cdsEnd + 1);
				String seq = reference.getSequence(chr, utrStart, exon.getEnd());
				utr.append(Sequence.reverseComplement(seq.toUpperCase()));
			}
		}
		return utr.toString();
	}

	/** Build the 3' UTR sequence from FASTA for this transcript. */
	private String build3PrimeUtr(TranscriptModel transcript, String chr) {
		StringBuilder utr = new StringBuilder();
		int cdsEnd = transcript.getCdsEnd();
		int cdsStart = transcript.getCdsStart();

		if (transcript.isPositiveStrand()) {
			// 3'UTR: exonic regions after CDS end
			for (ExonModel exon : transcript.getExons()) {
				if (exon.getEnd() <= cdsEnd) continue;
				int utrStart = Math.max(exon.getStart(), cdsEnd + 1);
				String seq = reference.getSequence(chr, utrStart, exon.getEnd());
				utr.append(seq.toUpperCase());
			}
		} else {
			// 3'UTR on - strand: exonic regions before CDS start (reverse complement)
			for (int i = transcript.getExons().size() - 1; i >= 0; i--) {
				ExonModel exon = transcript.getExons().get(i);
				if (exon.getStart() >= cdsStart) continue;
				int utrEnd = Math.min(exon.getEnd(), cdsStart - 1);
				String seq = reference.getSequence(chr, exon.getStart(), utrEnd);
				utr.append(Sequence.reverseComplement(seq.toUpperCase()));
			}
		}
		return utr.toString();
	}

	/**
	 * Check if stop codon is overlapped but NOT altered (stop_retained).
	 */
	public boolean isStopRetained(TranscriptModel transcript, String chr, int variantStart, int variantEnd) {
		try {
			int stopLow, stopHigh;
			if (transcript.isPositiveStrand()) {
				stopHigh = transcript.getCdsEnd();
				stopLow = stopHigh - 2;
			} else {
				stopLow = transcript.getCdsStart();
				stopHigh = stopLow + 2;
			}
			if (variantStart > stopHigh || variantEnd < stopLow) return false;
			String cds = buildCdsSequence(transcript, chr);
			if (cds == null || cds.length() < 3) return false;
			if (!CodonTable.isStop(cds.substring(cds.length() - 3))) return false;
			return !isStopAltered(transcript, chr, variantStart, variantEnd);
		} catch (Exception e) {
			return false;
		}
	}

	private String applyIndelToCds(String cds, int cdsPos, String vepAllele, String refAllele,
			boolean isDeletion, TranscriptModel transcript, int indelLength) {
		return applyIndelToCds(cds, cdsPos, vepAllele, refAllele, isDeletion, transcript, indelLength, null);
	}

	/**
	 * Apply indel to CDS, optionally appending 3'UTR.
	 * VEP _get_alternate_cds (TranscriptVariationAllele.pm line 2303-2348) builds
	 * upstream + allele + downstream where downstream extends through 3'UTR.
	 */
	private String applyIndelToCds(String cds, int cdsPos, String vepAllele, String refAllele,
			boolean isDeletion, TranscriptModel transcript, int indelLength, String utr3) {
		try {
			String seq = (utr3 != null) ? cds + utr3 : cds;
			if (isDeletion) {
				int delStart = cdsPos - 1;
				int delLen = indelLength;
				if (delStart + delLen > seq.length()) delLen = seq.length() - delStart;
				// VEP _get_alternate_cds: upstream + allele_seq + downstream
				// For pure deletion (vepAllele="-"): allele_seq is empty
				// For complex variant (vepAllele="G"): allele_seq replaces the deleted region
				String replaceSeq = "-".equals(vepAllele) ? "" :
					(transcript.isPositiveStrand() ? vepAllele : Sequence.reverseComplement(vepAllele));
				return seq.substring(0, delStart) + replaceSeq + seq.substring(delStart + delLen);
			} else {
				int insPos;
				if (transcript.isPositiveStrand()) {
					insPos = cdsPos - 1;
				} else {
					insPos = cdsPos;
				}
				String insertSeq = transcript.isPositiveStrand() ? vepAllele : Sequence.reverseComplement(vepAllele);
				if (insPos < 0) insPos = 0;
				if (insPos > seq.length()) insPos = seq.length();
				return seq.substring(0, insPos) + insertSeq + seq.substring(insPos);
			}
		} catch (Exception e) {
			return null;
		}
	}

	int genomicToCdsPosition(TranscriptModel transcript, int genomicPos) {
		List<CdsSegment> segments = transcript.getCdsSegments();
		int cdsPos = 0;

		if (transcript.isPositiveStrand()) {
			// Apply phase offset from first CDS segment
			cdsPos -= segments.get(0).getPhase();
			for (CdsSegment seg : segments) {
				if (genomicPos >= seg.getStart() && genomicPos <= seg.getEnd()) {
					return cdsPos + (genomicPos - seg.getStart()) + 1;
				}
				cdsPos += seg.getLength();
			}
		} else {
			// Minus strand: CDS is read in reverse genomic order
			cdsPos -= segments.get(segments.size() - 1).getPhase();
			for (int i = segments.size() - 1; i >= 0; i--) {
				CdsSegment seg = segments.get(i);
				if (genomicPos >= seg.getStart() && genomicPos <= seg.getEnd()) {
					return cdsPos + (seg.getEnd() - genomicPos) + 1;
				}
				cdsPos += seg.getLength();
			}
		}

		return -1;
	}

	/**
	 * VEP partial_codon (VariationEffect.pm line 1389-1414):
	 * Returns true if the variant falls in an incomplete terminal codon.
	 */
	private boolean isPartialCodon(TranscriptModel transcript, int cdsPos, int cdsLength) {
		int remainder = cdsLength % 3;
		if (remainder == 0) return false;
		int codonCdsStart = ((cdsPos - 1) / 3) * 3 + 1;
		int lastCodonLength = cdsLength - (codonCdsStart - 1);
		return lastCodonLength < 3 && lastCodonLength > 0;
	}

	String buildCdsSequence(TranscriptModel transcript, String chr) {
		StringBuilder cds = new StringBuilder();

		if (transcript.isPositiveStrand()) {
			for (CdsSegment seg : transcript.getCdsSegments()) {
				String seq = reference.getSequence(chr, seg.getStart(), seg.getEnd());
				cds.append(seq.toUpperCase());
			}
		} else {
			List<CdsSegment> segments = transcript.getCdsSegments();
			for (int i = segments.size() - 1; i >= 0; i--) {
				CdsSegment seg = segments.get(i);
				String seq = reference.getSequence(chr, seg.getStart(), seg.getEnd());
				cds.append(Sequence.reverseComplement(seq.toUpperCase()));
			}
		}

		// Apply phase offset
		int phase;
		if (transcript.isPositiveStrand()) {
			phase = transcript.getCdsSegments().get(0).getPhase();
		} else {
			phase = transcript.getCdsSegments().get(transcript.getCdsSegments().size() - 1).getPhase();
		}
		if (phase > 0 && phase < cds.length()) {
			return cds.substring(phase);
		}

		return cds.toString();
	}

	int computeCdnaPosition(TranscriptModel transcript, int genomicPos) {
		int cdnaPos = 0;
		if (transcript.isPositiveStrand()) {
			for (var exon : transcript.getExons()) {
				if (genomicPos >= exon.getStart() && genomicPos <= exon.getEnd()) {
					return cdnaPos + (genomicPos - exon.getStart()) + 1;
				}
				cdnaPos += (exon.getEnd() - exon.getStart() + 1);
			}
		} else {
			for (int i = transcript.getExons().size() - 1; i >= 0; i--) {
				var exon = transcript.getExons().get(i);
				if (genomicPos >= exon.getStart() && genomicPos <= exon.getEnd()) {
					return cdnaPos + (exon.getEnd() - genomicPos) + 1;
				}
				cdnaPos += (exon.getEnd() - exon.getStart() + 1);
			}
		}
		return -1;
	}

	private static String formatCodon(String codon, int variantPos) {
		StringBuilder sb = new StringBuilder(3);
		for (int i = 0; i < 3; i++) {
			char c = codon.charAt(i);
			if (i == variantPos) {
				sb.append(Character.toUpperCase(c));
			} else {
				sb.append(Character.toLowerCase(c));
			}
		}
		return sb.toString();
	}

	public static class CodingResult {
		private String consequence;
		private int cdsPosition;
		private int cdsEnd;
		private int proteinPosition;
		private int cdnaPosition;
		private int cdnaEnd;
		private char refAA;
		private char altAA;
		private String refCodon;
		private String altCodon;

		// VEP _translateable_seq and _get_alternate_cds — needed for _stop_loss_extra_AA
		private String cdsSequence;     // ref CDS (_translateable_seq)
		private String altCdsSequence;  // alt CDS (_get_alternate_cds)
		public String getCdsSequence() { return cdsSequence; }
		public void setCdsSequence(String v) { this.cdsSequence = v; }
		public String getAltCdsSequence() { return altCdsSequence; }
		public void setAltCdsSequence(String v) { this.altCdsSequence = v; }

		public String getConsequence() { return consequence; }
		public void setConsequence(String consequence) { this.consequence = consequence; }
		public int getCdsPosition() { return cdsPosition; }
		public void setCdsPosition(int cdsPosition) { this.cdsPosition = cdsPosition; }
		public int getCdsEnd() { return cdsEnd; }
		public void setCdsEnd(int cdsEnd) { this.cdsEnd = cdsEnd; }
		public int getProteinPosition() { return proteinPosition; }
		public void setProteinPosition(int proteinPosition) { this.proteinPosition = proteinPosition; }
		public int getCdnaPosition() { return cdnaPosition; }
		public void setCdnaPosition(int cdnaPosition) { this.cdnaPosition = cdnaPosition; }
		public int getCdnaEnd() { return cdnaEnd; }
		public void setCdnaEnd(int cdnaEnd) { this.cdnaEnd = cdnaEnd; }
		private int hgvsProteinPosition;
		public int getHgvsProteinPosition() { return hgvsProteinPosition > 0 ? hgvsProteinPosition : proteinPosition; }
		public void setHgvsProteinPosition(int pos) { this.hgvsProteinPosition = pos; }
		public char getRefAA() { return refAA; }
		public void setRefAA(char refAA) { this.refAA = refAA; }
		public char getAltAA() { return altAA; }
		public void setAltAA(char altAA) { this.altAA = altAA; }
		public String getRefCodon() { return refCodon; }
		public void setRefCodon(String refCodon) { this.refCodon = refCodon; }
		public String getAltCodon() { return altCodon; }
		public void setAltCodon(String altCodon) { this.altCodon = altCodon; }

		// HGVSp clip_alleles results
		private String clippedRefPeptide;
		private String clippedAltPeptide;
		private String hgvsType; // "=", ">", "fs", "del", "ins", "dup", "delins"
		private int hgvsProteinEnd;
		private String fsTerCount; // number or "?"
		private String extTerCount; // number or "?"
		private char flankLeftAA;
		private char flankRightAA;

		public String getClippedRefPeptide() { return clippedRefPeptide; }
		public void setClippedRefPeptide(String v) { this.clippedRefPeptide = v; }
		public String getClippedAltPeptide() { return clippedAltPeptide; }
		public void setClippedAltPeptide(String v) { this.clippedAltPeptide = v; }
		public String getHgvsType() { return hgvsType; }
		public void setHgvsType(String v) { this.hgvsType = v; }
		public int getHgvsProteinEnd() { return hgvsProteinEnd; }
		public void setHgvsProteinEnd(int v) { this.hgvsProteinEnd = v; }
		public String getFsTerCount() { return fsTerCount; }
		public void setFsTerCount(String v) { this.fsTerCount = v; }
		public String getExtTerCount() { return extTerCount; }
		public void setExtTerCount(String v) { this.extTerCount = v; }
		public char getFlankLeftAA() { return flankLeftAA; }
		public void setFlankLeftAA(char v) { this.flankLeftAA = v; }
		public char getFlankRightAA() { return flankRightAA; }
		public void setFlankRightAA(char v) { this.flankRightAA = v; }

		private String aminoAcids;
		private String codons;
		private HgvsNotation hgvsNotation;
		public HgvsNotation getHgvsNotation() { return hgvsNotation; }
		public void setHgvsNotation(HgvsNotation v) { this.hgvsNotation = v; }

		public String getAminoAcids() {
			if (aminoAcids != null) return aminoAcids;
			if (refAA == 0 && altAA == 0) return null;
			if (refAA == altAA) {
				return String.valueOf(refAA);
			}
			return String.valueOf(refAA) + "/" + String.valueOf(altAA);
		}
		public void setAminoAcids(String aminoAcids) { this.aminoAcids = aminoAcids; }

		public String getCodons() {
			if (codons != null) return codons;
			if (refCodon == null || altCodon == null) return null;
			return refCodon + "/" + altCodon;
		}
		public void setCodons(String codons) { this.codons = codons; }
	}

	// ===================================================================
	// VEP HGVSp method ports — each matches a specific Perl subroutine
	// ===================================================================

	/** Intermediate state for _clip_alleles, passed between VEP methods. */
	public static class HgvsNotation {
		public String ref;
		public String alt;
		public String preseq;
		public String originalRef;
		public int start;
		public int end;
		public String type;
	}

	/**
	 * VEP codon() — TranscriptVariationAllele.pm line 790-868.
	 * Extracts the codon string from the CDS at translation_start..translation_end,
	 * adjusted for the indel length.
	 *
	 * @param cds The full CDS sequence (ref or alt)
	 * @param codonCdsStart0 0-based CDS start of the codon range
	 * @param extractLen Number of bases to extract
	 * @return The codon string, or "-" if empty
	 */
	private String vepCodon(String cds, int codonCdsStart0, int extractLen) {
		if (cds == null || extractLen <= 0 || codonCdsStart0 < 0) return "-";
		String codon = safeSubstring(cds, codonCdsStart0, codonCdsStart0 + extractLen);
		return (codon == null || codon.isEmpty()) ? "-" : codon;
	}

	/**
	 * VEP peptide() — TranscriptVariationAllele.pm line 684-778.
	 * Translates a codon string to its peptide. Handles partial codons (→ X).
	 */
	private String vepPeptide(String codon) {
		if (codon == null || codon.equals("-") || codon.isEmpty()) return "-";
		return translateCds(codon);
	}

	/**
	 * VEP _clip_alleles — TranscriptVariationAllele.pm line 2102-2203.
	 * Strips matching leading and trailing AAs from ref and alt peptides.
	 * Prefix clip bounded by length(ref). Suffix clip bounded by remaining ref after prefix.
	 * Detects dup/ins/del/>/delins type from clipped result.
	 */
	public static HgvsNotation vepClipAlleles(String ref, String alt, int start, int end) {
		HgvsNotation n = new HgvsNotation();
		n.originalRef = ref;
		n.preseq = "";
		String checkRef = ref;
		String checkAlt = alt;
		int checkStart = start;
		int checkEnd = end;

		// Line 2118-2139: prefix clip, bounded by length(ref)
		for (int p = 0; p < ref.length(); p++) {
			if (checkRef.isEmpty() || checkAlt.isEmpty()) break;
			char nextRef = checkRef.charAt(0);
			char nextAlt = checkAlt.charAt(0);
			// Line 2122-2128: both start with '*' → synonymous
			if (p == 0 && nextRef == '*' && nextAlt == '*') {
				n.type = "=";
				n.ref = checkRef; n.alt = checkAlt;
				n.start = checkStart; n.end = checkEnd;
				return n;
			}
			if (nextRef == nextAlt) {
				checkStart++;
				n.preseq += nextRef;
				checkRef = checkRef.substring(1);
				checkAlt = checkAlt.substring(1);
			} else {
				break;
			}
		}

		// Line 2141-2155: suffix clip, bounded by length(check_ref)
		int suffLen = checkRef.length();
		for (int q = 0; q < suffLen; q++) {
			if (checkRef.isEmpty() || checkAlt.isEmpty()) break;
			if (checkRef.charAt(checkRef.length() - 1) == checkAlt.charAt(checkAlt.length() - 1)) {
				checkRef = checkRef.substring(0, checkRef.length() - 1);
				checkAlt = checkAlt.substring(0, checkAlt.length() - 1);
				checkEnd--;
			} else {
				break;
			}
		}

		// Line 2157-2162: write back
		n.ref = checkRef;
		n.alt = checkAlt;
		n.start = checkStart;
		n.end = checkEnd;

		// Line 2164-2199: type determination
		if (checkRef.equals(checkAlt)) {
			n.type = "=";
		} else if (!checkRef.equals("-") && checkRef.length() == 1 && checkAlt.length() == 1
				&& !checkAlt.equals(checkRef)) {
			n.type = ">";
		} else if (checkRef.isEmpty() && checkAlt.length() >= 1) {
			// Line 2182-2194: insertion or dup
			String prevStr = (n.preseq.length() >= checkAlt.length())
				? n.preseq.substring(n.preseq.length() - checkAlt.length()) : "";
			if (prevStr.equals(checkAlt)) {
				n.type = "dup";
				n.start -= checkAlt.length(); // Line 2187
			} else {
				n.type = "ins";
			}
		} else if (checkRef.length() >= 1 && checkAlt.isEmpty()) {
			n.type = "del";
		} else {
			n.type = "delins";
		}

		return n;
	}

	/**
	 * VEP _get_hgvs_protein_type — TranscriptVariationAllele.pm line 1961-1995.
	 * Overrides the type from _clip_alleles based on peptide content.
	 * Frameshift always wins. Then checks ref/alt for ins/del/>/delins.
	 */
	private String vepGetHgvsProteinType(HgvsNotation n, boolean isFrameshift) {
		// Line 1967-1970
		if (isFrameshift) return "fs";

		// Line 1972-1995: check peptides
		String ref = n.ref.replace("*", "X");
		String alt = n.alt.replace("*", "X");

		if (ref.equals("-") || ref.isEmpty()) return "ins";
		if (alt.isEmpty() || alt.equals("-")) return "del";
		if (ref.length() == 1 && alt.length() == 1) return ">";
		if ((ref.length() != alt.length()) || (ref.length() > 1 && alt.length() > 1)) return "delins";
		return ">";
	}

	/**
	 * VEP _get_fs_peptides — TranscriptVariationAllele.pm line 2229-2274.
	 * For frameshifts: resets start to translation_start, translates FULL alt CDS,
	 * walks ref vs alt translation to find first differing AA.
	 * May change type to "del" (stop deletion) or "=" (stop maintained).
	 */
	private HgvsNotation vepGetFsPeptides(HgvsNotation n, String cdsSequence, String altCds,
			int translationStart) {
		if (altCds == null) return null;

		// Line 2242: translate full alt CDS
		// VEP uses BioPerl translate() which only translates COMPLETE codons (no X for partials)
		String altTrans = translateCdsWholeOnly(altCds);

		// Line 2245-2247: get full ref peptide + appended stop
		// VEP uses _peptide() which also uses BioPerl translate (complete codons only)
		String refTrans = translateCdsWholeOnly(cdsSequence) + "*";

		// Line 2249: reset start to translation_start
		n.start = translationStart;

		// Line 2251-2254: deletion of stop, no further AA in alt seq
		if (n.start > altTrans.length()) {
			n.alt = "del";
			n.type = "del";
			return n;
		}

		// Line 2257-2271: walk from start, find first differing position
		while (n.start <= altTrans.length()) {
			char refAA = (n.start - 1 < refTrans.length()) ? refTrans.charAt(n.start - 1) : '?';
			char altAA = altTrans.charAt(n.start - 1);
			n.ref = String.valueOf(refAA);
			n.alt = String.valueOf(altAA);

			// Line 2263-2266: both stop → synonymous
			if (refAA == '*' && altAA == '*') {
				n.type = "=";
				return n;
			}

			// Line 2269: stop when they differ
			if (refAA != altAA) break;
			n.start++;
		}

		return n;
	}

	/**
	 * VEP _check_peptides_post_var + _shift_3prime — line 2482-2553.
	 * Shifts a variant 3' along the peptide if the variant sequence matches downstream ref.
	 * For "ins": checks alt against downstream. For "del": checks ref against downstream.
	 * Uses _peptide() (excludes stop) for bounds check.
	 *
	 * @param n The notation (must have type set to "ins" or "del")
	 * @param hgvsType The HGVS type ("ins" or "del")
	 * @param refPepNoStop Full ref peptide excluding stop codon
	 */
	private void vepShift3Prime(HgvsNotation n, String hgvsType, String refPepNoStop) {
		// Line 2487-2491: get post-variant peptide
		int postPos = n.end + 1; // 1-based
		// Line 2288: guard — return if position past peptide end
		if (postPos <= 0 || postPos > refPepNoStop.length()) return;

		String postSeq = refPepNoStop.substring(postPos - 1);

		// Line 2509-2518: select sequence to check based on type
		String seqToCheck;
		if ("ins".equals(hgvsType)) {
			seqToCheck = n.alt;     // Line 2511
		} else if ("del".equals(hgvsType)) {
			seqToCheck = n.ref;     // Line 2514
		} else {
			return;                  // Line 2516-2518
		}
		if (seqToCheck == null || seqToCheck.isEmpty()) return;

		// Line 2528-2547: shift loop
		for (int nn = 0; nn + seqToCheck.length() <= postSeq.length(); nn++) {
			if (seqToCheck.charAt(0) == postSeq.charAt(nn)) {
				n.start++;
				n.end++;
				// Line 2541-2542: rotate — remove start, append to end
				seqToCheck = seqToCheck.substring(1) + seqToCheck.charAt(0);
			} else {
				break;
			}
		}
		// Line 2549-2550: write back to correct field
		if ("ins".equals(hgvsType)) {
			n.alt = seqToCheck;
		} else if ("del".equals(hgvsType)) {
			n.ref = seqToCheck;
		}
	}

	/**
	 * VEP _check_for_peptide_duplication — TranscriptVariationAllele.pm line 2350-2383.
	 * Checks if inserted peptide matches the upstream ref at the current position.
	 * Uses full reference translation + preseq for the upstream check.
	 */
	private void vepCheckForPeptideDuplication(HgvsNotation n, String refPepNoStop) {
		// Line 2361-2362: build upstream
		int upLen = Math.min(n.start - 1, refPepNoStop.length());
		String upstream = refPepNoStop.substring(0, upLen);
		// Line 2365: append preseq
		if (n.preseq != null && !n.preseq.isEmpty()) {
			upstream += n.preseq;
		}

		// Line 2368
		int testNewStart = n.start - n.alt.length() - 1; // 0-based index

		// Line 2370-2380
		if (testNewStart >= 0 && testNewStart + n.alt.length() <= upstream.length()) {
			String testSeq = upstream.substring(testNewStart, testNewStart + n.alt.length());
			if (testSeq.equals(n.alt)) {
				n.type = "dup";
				n.end = n.start - 1;              // Line 2375
				n.start -= n.alt.length();          // Line 2376
			}
		}
	}

	/**
	 * VEP _get_del_peptides — TranscriptVariationAllele.pm line 2453-2478.
	 * For deletions: translates full alt CDS from translation_start, clips
	 * the ref/alt FULL translations, and produces the clipped peptides.
	 */
	private void vepGetDelPeptides(HgvsNotation n, String cdsSequence, String altCds,
			int translationStart) {
		if (altCds == null) return;

		// Line 2463-2464: translate alt CDS from translation_start
		String altTrans = translateCds(altCds);
		int start0 = translationStart - 1;
		String altFromStart = (start0 < altTrans.length()) ? altTrans.substring(start0) : "";
		// Line 2465: split on stop, take before stop
		int stopIdx = altFromStart.indexOf('*');
		n.alt = (stopIdx >= 0) ? altFromStart.substring(0, stopIdx) : altFromStart;

		// Line 2468: ref from full translation from translation_start
		String refTrans = translateCds(cdsSequence);
		n.ref = (start0 < refTrans.length()) ? refTrans.substring(start0) : "";

		// Line 2470: reset start
		n.start = translationStart;

		// Line 2472: re-clip the FULL peptides (not short ones)
		HgvsNotation reclipped = vepClipAlleles(n.ref, n.alt, n.start, n.start + n.ref.length() - 1);
		n.ref = reclipped.ref;
		n.alt = reclipped.alt;
		n.start = reclipped.start;
		n.end = reclipped.end;
		n.preseq = reclipped.preseq;
		// Type from reclip
		if (reclipped.type != null) n.type = reclipped.type;
	}

	/**
	 * VEP _get_surrounding_peptides — TranscriptVariationAllele.pm line 2276-2299.
	 * Gets ref peptides flanking an insertion position.
	 *
	 * @param refPep Full reference peptide (from _peptide(), may need stop appended)
	 * @param pos 1-based position
	 * @param originalRef Original ref before clipping (for stop handling)
	 * @param length Number of AAs to return
	 */
	private String vepGetSurroundingPeptides(String refPep, int pos, String originalRef, int length) {
		// Line 2284-2285: append original_ref if it starts with *
		String ref = refPep;
		if (originalRef != null && originalRef.startsWith("*")) {
			ref += originalRef;
		}
		// Line 2288: guard
		if (ref.length() <= pos) return null;
		// Line 2291-2296
		return ref.substring(pos - 1, Math.min(pos - 1 + length, ref.length()));
	}

	/**
	 * VEP _stop_loss_extra_AA — TranscriptVariationAllele.pm line 2386-2435.
	 * Counts AAs from variant position to next stop in alt translation.
	 *
	 * @param altCds Alt CDS sequence
	 * @param refPep Ref peptide (from _peptide())
	 * @param refVarPos 0-based first affected AA position
	 * @param test "fs" for frameshifts, null/other for non-fs
	 */
	private String vepStopLossExtraAA(String altCds, String refPep, int refVarPos, String test) {
		if (refVarPos <= 0) return null;
		if (altCds == null) return null;

		// Line 2401: translate alt CDS (BioPerl translate — whole codons only)
		String altTrans = translateCdsWholeOnly(altCds);
		// Line 2403-2404: ref length
		int refLen = (refPep != null) ? refPep.length() : 0;

		// Line 2412: find stop in alt translation
		int stopPos = altTrans.indexOf('*');
		if (stopPos < 0) return null; // no stop found

		int extraAA;
		if ("fs".equals(test)) {
			// Line 2416-2417: frameshift — count from first AA to stop
			extraAA = (stopPos + 1) - refVarPos; // $+[0] is 1-based position of char after match
		} else {
			// Line 2422: non-fs — count from ref stop to new stop
			extraAA = stopPos - refLen; // $+[0] - 1 - refLen
		}

		// Line 2428-2433
		if (extraAA > 0) {
			return String.valueOf(extraAA);
		}
		return null;
	}

	/**
	 * VEP _get_hgvs_protein_format — TranscriptVariationAllele.pm line 1818-1958.
	 * Formats the final HGVSp string from the notation hash.
	 * Switches on notation TYPE (=, >, fs, del, ins, delins, dup), NOT consequence type.
	 *
	 * @param n The HgvsNotation with type, ref, alt, start, end set
	 * @param proteinId The protein accession (e.g. UniProtKB:P39712)
	 * @param isStopLost Whether consequence includes stop_lost
	 * @param altCds Alt CDS for _stop_loss_extra_AA
	 * @param cdsSequence Ref CDS for _stop_loss_extra_AA
	 * @return The formatted HGVSp string, or null
	 */
	public String vepGetHgvsProteinFormat(HgvsNotation n, String proteinId, boolean isStopLost,
			boolean isStartLost, String altCds, String cdsSequence) {

		if (n == null || n.type == null) return null;

		String prefix = (proteinId != null ? proteinId : "") + ":p.";

		// Convert ref/alt to 3-letter code (VEP line 2067-2071)
		String ref3 = to3Letter(n.ref);
		String alt3 = to3Letter(n.alt);

		// VEP line 2072: alt = "del" if alt == "-"
		if ("-".equals(n.alt) || n.alt.isEmpty()) alt3 = "del";

		// VEP line 2075-2078: start_lost overrides everything
		if (isStartLost) {
			alt3 = "?";
			// type becomes "" — just output ref + start + alt
			return prefix + ref3 + n.start + alt3;
		}

		// Line 1831-1833: synonymous (ref == alt, not fs, not ins)
		if (ref3.equals(alt3) && !"fs".equals(n.type) && !"ins".equals(n.type)) {
			return prefix + ref3 + n.start + "=";
		}

		// Line 1836-1857: stop_lost with del or >
		if (isStopLost && ("del".equals(n.type) || ">".equals(n.type))) {
			String firstAlt = alt3.length() >= 3 ? alt3.substring(0, 3) : alt3;
			String aaTilStop = vepStopLossExtraAA(altCds, cdsSequence != null ? translateCds(cdsSequence) : null, n.start - 1, null);
			if (aaTilStop == null) aaTilStop = "?";
			String extPart = firstAlt + "extTer" + aaTilStop;

			if (ref3.length() > 3 && "del".equals(n.type)) {
				String refFirst = ref3.substring(0, 3);
				String refLast = ref3.substring(ref3.length() - 3);
				return prefix + refFirst + n.start + "_" + refLast + n.end + extPart;
			} else {
				return prefix + ref3 + n.start + extPart;
			}
		}

		// Line 1859-1871: dup
		if ("dup".equals(n.type)) {
			if (n.start < n.end) {
				String altFirst = alt3.length() >= 3 ? alt3.substring(0, 3) : alt3;
				String altLast = alt3.length() >= 3 ? alt3.substring(alt3.length() - 3) : alt3;
				return prefix + altFirst + n.start + "_" + altLast + n.end + "dup";
			} else {
				return prefix + alt3 + n.start + "dup";
			}
		}

		// Line 1873-1876: substitution >
		if (">".equals(n.type)) {
			return prefix + ref3 + n.start + alt3;
		}

		// Line 1878-1913: delins or ins
		if ("delins".equals(n.type) || "ins".equals(n.type)) {
			// Line 1881: truncate alt after Ter
			alt3 = alt3.replaceAll("Ter\\w+", "Ter");

			// Line 1883-1891: first and last ref
			String refFirst = ref3.length() >= 3 ? ref3.substring(0, 3) : ref3;
			String refLast;
			if (ref3.endsWith("X") || ref3.endsWith("Ter")) {
				refLast = "Ter";
			} else {
				refLast = ref3.length() >= 3 ? ref3.substring(ref3.length() - 3) : ref3;
			}

			// Line 1893-1899: stop in ref → add extTer
			if (n.ref != null && n.ref.endsWith("X")) {
				String aaTilStop = vepStopLossExtraAA(altCds, cdsSequence != null ? translateCds(cdsSequence) : null, n.start - 1, "loss");
				if (aaTilStop != null) {
					alt3 += "extTer" + aaTilStop;
				}
			}

			// Line 1902-1912: format
			if (n.start == n.end && "delins".equals(n.type)) {
				return prefix + refFirst + n.start + n.type + alt3;
			} else {
				int s = Math.min(n.start, n.end);
				int e = Math.max(n.start, n.end);
				return prefix + refFirst + s + "_" + refLast + e + n.type + alt3;
			}
		}

		// Line 1915-1930: frameshift
		if ("fs".equals(n.type)) {
			if ("Ter".equals(alt3)) {
				// Line 1917-1919: stop gained immediately
				return prefix + ref3 + n.start + alt3;
			} else {
				// Line 1921-1929: count AA until next stop
				String refPep = (cdsSequence != null) ? translateCds(cdsSequence) : null;
				String aaTilStop = vepStopLossExtraAA(altCds, refPep, n.start - 1, "fs");
				if (aaTilStop == null) aaTilStop = "?";
				return prefix + ref3 + n.start + alt3 + "fsTer" + aaTilStop;
			}
		}

		// Line 1932-1942: del
		if ("del".equals(n.type)) {
			if (ref3.length() > 3) {
				String refFirst = ref3.substring(0, 3);
				String refLast = ref3.substring(ref3.length() - 3);
				return prefix + refFirst + n.start + "_" + refLast + n.end + "del";
			} else {
				return prefix + ref3 + n.start + "del";
			}
		}

		// Line 1944-1946: start != end
		if (n.start != n.end) {
			return prefix + ref3 + n.start + "_" + alt3 + n.end;
		}

		// Line 1948-1951: default substitution
		return prefix + ref3 + n.start + alt3;
	}

	/**
	 * VEP TranscriptVariation::codon_position — TranscriptVariation.pm line 287-307.
	 * Returns 1-based position within the codon.
	 * Formula: ((cdna_start - tran_cdna_start + phase_offset) % 3) + 1
	 *
	 * @param cdsPosition 1-based CDS position of the variant
	 * @param transcript The transcript model
	 * @return 1-based codon position (1, 2, or 3)
	 */
	/**
	 * VEP TranscriptVariation::codon_position — TranscriptVariation.pm line 287-307.
	 * Exact port: ((cdna_start - tran_cdna_start + phase_offset) % 3) + 1
	 *
	 * @param cdnaStart cDNA position of the variant (1-based, from transcript start)
	 * @param transcript The transcript model
	 * @return 1-based codon position (1, 2, or 3), or 0 if undefined
	 */
	public static int vepCodonPosition(int cdnaStart, org.alliancegenome.vep.model.TranscriptModel transcript) {
		// VEP line 294: tran_cdna_start = transcript->cdna_coding_start
		int tranCdnaStart = transcript.getCdnaCodingStart();
		if (tranCdnaStart <= 0) tranCdnaStart = 1; // fallback

		// VEP line 297-299: exon_phase = transcript->start_Exon->phase
		int exonPhase = transcript.getStartExonPhase();
		int phaseOffset = exonPhase > 0 ? exonPhase : 0;

		// VEP line 302
		return ((cdnaStart - tranCdnaStart + phaseOffset) % 3) + 1;
	}

	/** Convert 1-letter peptide to 3-letter code. Handles multi-AA strings. */
	private String to3Letter(String oneLetterPep) {
		if (oneLetterPep == null || oneLetterPep.isEmpty() || "-".equals(oneLetterPep)) return oneLetterPep;
		if ("del".equals(oneLetterPep)) return "del";
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < oneLetterPep.length(); i++) {
			sb.append(org.alliancegenome.vep.bio.AminoAcid.threeLetterCode(oneLetterPep.charAt(i)));
		}
		return sb.toString();
	}

	// ===================================================================
	// Additional method ports from TranscriptVariationAllele.pm
	// ===================================================================

	/**
	 * VEP affects_peptide — line 594-597.
	 * Check if this variant changes the resultant peptide sequence.
	 */
	public static boolean affectsPeptide(String consequence) {
		if (consequence == null) return false;
		return consequence.contains("stop") || consequence.contains("missense")
			|| consequence.contains("frameshift") || consequence.contains("inframe")
			|| consequence.contains("initiator") || consequence.contains("start_lost");
	}

	/**
	 * VEP pep_allele_string — line 610-622.
	 * Return ref_pep/alt_pep or single pep if synonymous.
	 */
	public static String pepAlleleString(String refPep, String altPep) {
		if (refPep == null || altPep == null) return null;
		return refPep.equals(altPep) ? refPep : refPep + "/" + altPep;
	}

	/**
	 * VEP codon_allele_string — line 634-644.
	 * Return ref_codon/alt_codon.
	 */
	public static String codonAlleleString(String refCodon, String altCodon) {
		if (refCodon == null || altCodon == null) return null;
		return refCodon + "/" + altCodon;
	}

	/**
	 * VEP display_codon — TranscriptVariationAllele.pm line 884-915.
	 * Exact port. Lowercase codon, then uppercase the variant bases.
	 *
	 * @param codon The codon string from codon()
	 * @param featureSeq The allele's feature_seq (variant bases, or "-" for indel)
	 * @param codonPosition 1-based position within the codon (transcript_variation->codon_position)
	 * @return Display codon string, or null
	 */
	public static String displayCodon(String codon, String featureSeq, int codonPosition) {
		if (codon == null) return null;
		// VEP line 861-862: if length($codon) < 1 → codon = '-'
		if (codon.isEmpty() || "-".equals(codon)) return "-";

		// Line 894
		String displayCodon = codon.toLowerCase();

		// Line 896
		if (codonPosition > 0) {
			// Line 899: if this allele is an indel then just return all lowercase
			if (featureSeq != null && !"-".equals(featureSeq) && !featureSeq.isEmpty()) {
				// Line 902: codon_position is 1-based
				int pos = codonPosition - 1;
				// Line 904
				int len = featureSeq.length();
				// Line 906: substr($display_codon, $pos, $len) = uc substr(...)
				StringBuilder sb = new StringBuilder(displayCodon);
				int end = Math.min(pos + len, sb.length());
				for (int i = pos; i < end; i++) {
					sb.setCharAt(i, Character.toUpperCase(sb.charAt(i)));
				}
				displayCodon = sb.toString();
			}
		}

		return displayCodon;
	}

	/**
	 * VEP display_codon_allele_string — line 658-673.
	 * Return ref_display_codon/alt_display_codon.
	 */
	public static String displayCodonAlleleString(String refDisplayCodon, String altDisplayCodon) {
		if (refDisplayCodon == null || altDisplayCodon == null) return null;
		return refDisplayCodon + "/" + altDisplayCodon;
	}

	/**
	 * VEP _get_alternate_cds — TranscriptVariationAllele.pm line 2302-2348.
	 * Build alternate CDS by splicing the alt allele into the ref CDS.
	 * This is equivalent to our existing applyIndelToCds method.
	 *
	 * @param cdsSequence Reference CDS (_translateable_seq)
	 * @param cdsStart CDS position of variant start (1-based)
	 * @param cdsEnd CDS position of variant end (1-based)
	 * @param altAllele Alt allele in CDS strand orientation (empty for deletion)
	 * @param utr3 3' UTR sequence to append
	 * @return Alt CDS + UTR sequence
	 */
	public static String getAlternateCds(String cdsSequence, int cdsStart, int cdsEnd,
			String altAllele, String utr3) {
		if (cdsSequence == null) return null;
		// Line 2318-2319: upstream and downstream
		String upstream = (cdsStart > 1) ? cdsSequence.substring(0, cdsStart - 1) : "";
		String downstream = (cdsEnd <= cdsSequence.length()) ? cdsSequence.substring(cdsEnd) : "";
		// Line 2324: fix alt allele
		String alt = (altAllele != null) ? altAllele.replace("-", "") : "";
		// Line 2330: build alternate
		String alternateCds = upstream + alt + downstream;
		// Line 2331: trim incomplete codon
		int fullLen = (alternateCds.length() / 3) * 3;
		if (fullLen < alternateCds.length()) {
			alternateCds = alternateCds.substring(0, fullLen);
		}
		// Line 2337-2342: append UTR
		if (utr3 != null && !utr3.isEmpty()) {
			alternateCds += utr3;
		}
		return alternateCds;
	}

	/**
	 * VEP _trim_incomplete_codon — line 2438-2449.
	 * Trim trailing bases that don't form a complete codon.
	 */
	public static String trimIncompleteCodon(String seq) {
		if (seq == null) return null;
		int fullLen = seq.length() - (seq.length() % 3);
		if (fullLen == seq.length()) return seq;
		return seq.substring(0, fullLen);
	}
}
