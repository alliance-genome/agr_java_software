package org.alliancegenome.vep.annotation;

import java.util.ArrayList;
import java.util.List;

import org.alliancegenome.vep.bio.AminoAcid;
import org.alliancegenome.vep.bio.CodonTable;
import org.alliancegenome.vep.bio.Sequence;
import org.alliancegenome.vep.debug.Trace;
import org.alliancegenome.vep.model.CdsSegment;
import org.alliancegenome.vep.model.ExonModel;
import org.alliancegenome.vep.model.Mapper;
import org.alliancegenome.vep.model.TranscriptMapper;
import org.alliancegenome.vep.model.TranscriptModel;
import org.alliancegenome.vep.reference.ReferenceGenome;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class TranscriptVariationAllele {

	private final ReferenceGenome reference;
	public ReferenceGenome getReference() { return reference; }

	// Per-allele state fields (matching Perl's $bvfoa pattern)
	private BaseTranscriptVariation bvt;
	private TranscriptModel transcript;
	private String chr;
	private int variantStart;
	private int variantEnd;
	private String vepAllele;
	private String refAllele;

	// Lazy computation flag
	private boolean computed = false;

	// Fields promoted from CodingResult
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
	private String rawRefCodon;
	private String rawAltCodon;
	private String cdsSequence;
	private String altCdsSequence;
	private int hgvsProteinPosition;
	private String clippedRefPeptide;
	private String clippedAltPeptide;
	private String hgvsType;
	private int hgvsProteinEnd;
	private String fsTerCount;
	private String extTerCount;
	private char flankLeftAA;
	private char flankRightAA;
	private String aminoAcids;
	private String codons;
	private HgvsNotation hgvsNotation;

	/** Shared-instance constructor (for hgvsTranscript, vepGetHgvsProteinFormat, etc.) */
	public TranscriptVariationAllele(ReferenceGenome reference) {
		this.reference = reference;
	}

	/** Per-allele constructor matching Perl's $bvfoa creation pattern. */
	public TranscriptVariationAllele(TranscriptModel transcript,
			ReferenceGenome reference, String chr, int variantStart, int variantEnd,
			String vepAllele, String refAllele) {
		this.reference = reference;
		this.transcript = transcript;
		this.chr = chr;
		this.variantStart = variantStart;
		this.variantEnd = variantEnd;
		this.vepAllele = vepAllele;
		this.refAllele = refAllele;
	}

	/** Per-allele constructor accepting a pre-computed BaseTranscriptVariation. */
	public TranscriptVariationAllele(BaseTranscriptVariation bvt, TranscriptModel transcript,
			ReferenceGenome reference, String chr, int variantStart, int variantEnd,
			String vepAllele, String refAllele) {
		this.reference = reference;
		this.bvt = bvt;
		this.transcript = transcript;
		this.chr = chr;
		this.variantStart = variantStart;
		this.variantEnd = variantEnd;
		this.vepAllele = vepAllele;
		this.refAllele = refAllele;
	}

	public BaseTranscriptVariation getBvt() {
		ensureComputed();
		return bvt;
	}

	// --- Lazy computation ---

	private void ensureComputed() {
		if (!computed) {
			try {
				computeInternal();
			} catch (Exception e) {
				Trace.log("SILENT_CATCH_1", "error=%s at %s", e.toString(), e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "?");
				log.debug("Failed to compute TVA: {}", e.getMessage());
				Trace.log("ensureComputed.EXCEPTION", "error=%s at %s", e.toString(),
					e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "?");
			}
			computed = true;
		}
	}

	private void computeInternal() {
		if (bvt == null) {
			bvt = new BaseTranscriptVariation(transcript, variantStart, variantEnd);
		}
		annotateInternal(transcript, chr, variantStart, variantEnd, vepAllele, refAllele);
	}

	// --- Public getters (lazy-computed) ---

	public String getConsequence() { ensureComputed(); return consequence; }
	public int getCdsPosition() { ensureComputed(); return cdsPosition; }
	public int getCdsEnd() { ensureComputed(); return cdsEnd; }
	public int getProteinPosition() { ensureComputed(); return proteinPosition; }
	public int getCdnaPosition() { ensureComputed(); return cdnaPosition; }
	public int getCdnaEnd() { ensureComputed(); return cdnaEnd; }
	public char getRefAA() { ensureComputed(); return refAA; }
	public char getAltAA() { ensureComputed(); return altAA; }
	public String getRefCodon() { ensureComputed(); return refCodon; }
	public String getAltCodon() { ensureComputed(); return altCodon; }
	public String getRawRefCodon() { ensureComputed(); return rawRefCodon; }
	public String getRawAltCodon() { ensureComputed(); return rawAltCodon; }
	public String getCdsSequence() { ensureComputed(); return cdsSequence; }
	public String getAltCdsSequence() { ensureComputed(); return altCdsSequence; }
	public String getClippedRefPeptide() { ensureComputed(); return clippedRefPeptide; }
	public String getClippedAltPeptide() { ensureComputed(); return clippedAltPeptide; }
	public String getHgvsType() { ensureComputed(); return hgvsType; }
	public int getHgvsProteinEnd() { ensureComputed(); return hgvsProteinEnd; }
	public String getFsTerCount() { ensureComputed(); return fsTerCount; }
	public String getExtTerCount() { ensureComputed(); return extTerCount; }
	public char getFlankLeftAA() { ensureComputed(); return flankLeftAA; }
	public char getFlankRightAA() { ensureComputed(); return flankRightAA; }
	public HgvsNotation getHgvsNotation() { ensureComputed(); return hgvsNotation; }

	public int getHgvsProteinPosition() {
		ensureComputed();
		return hgvsProteinPosition > 0 ? hgvsProteinPosition : proteinPosition;
	}

	public String getAminoAcids() {
		ensureComputed();
		if (aminoAcids != null) return aminoAcids;
		if (refAA == 0 && altAA == 0) return null;
		if (refAA == altAA) {
			return String.valueOf(refAA);
		}
		return String.valueOf(refAA) + "/" + String.valueOf(altAA);
	}

	public String getCodons() {
		ensureComputed();
		if (codons != null) return codons;
		if (refCodon == null || altCodon == null) return null;
		return refCodon + "/" + altCodon;
	}

	private void annotateInternal(TranscriptModel transcript, String chr, int variantStart, int variantEnd,
			String vepAllele, String refAllele) {

		if (!isUnambiguousDna(vepAllele) || !isUnambiguousDna(refAllele)) {
			return;
		}

		boolean isDeletion = "-".equals(vepAllele);
		boolean isInsertion = "-".equals(refAllele);

		if (isDeletion || isInsertion) {
			annotateIndel(transcript, chr, variantStart, variantEnd, vepAllele, refAllele, isDeletion);
			return;
		}

		if (vepAllele.length() != refAllele.length()) {
			boolean netDeletion = vepAllele.length() < refAllele.length();
			annotateIndel(transcript, chr, variantStart, variantEnd, vepAllele, refAllele, netDeletion);
			return;
		}

		if (vepAllele.length() == 1 && refAllele.length() == 1) {
			annotateSNP(transcript, chr, variantStart, refAllele, vepAllele);
			return;
		}

		// Same-length multi-base substitution (MNP / delins). Route to annotateIndel,
		// which already extracts the full codon range via translation_start/end and
		// splices the allele properly (matches Perl TranscriptVariationAllele::codon
		// line 854-859 for the allele_len == vf_nt_len branch). Java previously
		// truncated MNPs to their first base and called annotateSNP, which dropped
		// the second affected codon entirely (e.g. GGC>AAT showed agC/agT instead
		// of agGCCc/agATTc).
		annotateIndel(transcript, chr, variantStart, variantEnd, vepAllele, refAllele, false);
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

	private void annotateSNP(TranscriptModel transcript, String chr, int pos, String refBase, String altBase) {
		int cdsPos = bvt.cdsStart();
		if (cdsPos < 0) return;

		int codonIndex = (cdsPos - 1) / 3;
		int posInCodon = (cdsPos - 1) % 3;

		String cdsSeq = BaseTranscriptVariation.translateableSeq(transcript, reference);
		if (cdsSeq == null || cdsPos > cdsSeq.length()) return;

		int codonStart = codonIndex * 3;
		if (codonStart + 3 > cdsSeq.length()) return;

		String rawCodon = cdsSeq.substring(codonStart, codonStart + 3);

		// VEP TranscriptVariationAllele::codon (line 793-885) splices the allele into
		// the translateable_seq at cds_start-1 before extracting the codon. This is done
		// for BOTH ref and alt alleles, each producing its own codon.
		// When startExonPhase != translationStartExonPhase (e.g. FBtr0079971 where the
		// transcript's first exon in tx order is different from the translation's start
		// exon), cds_start is computed from the transcript's first exon phase but the
		// translateable_seq is padded using the translation's start exon phase, causing
		// an off-by-one splice. Java replicates this by splicing BOTH ref and alt at the
		// same posInCodon (matching Perl's behavior exactly).
		String effectiveRef = transcript.isPositiveStrand() ? refBase : Sequence.reverseComplement(refBase);
		String effectiveAlt = transcript.isPositiveStrand() ? altBase : Sequence.reverseComplement(altBase);
		char[] refCodonChars = rawCodon.toCharArray();
		char[] altCodonChars = rawCodon.toCharArray();
		refCodonChars[posInCodon] = effectiveRef.charAt(0);
		altCodonChars[posInCodon] = effectiveAlt.charAt(0);
		String refCdn = new String(refCodonChars);
		String altCdn = new String(altCodonChars);

		char rAA = CodonTable.translate(refCdn);
		char aAA = CodonTable.translate(altCdn);

		this.cdsPosition = cdsPos;
		this.proteinPosition = bvt.translationStart();
		this.refAA = rAA;
		this.altAA = aAA;
		this.refCodon = formatCodon(refCdn, posInCodon);
		this.altCodon = formatCodon(altCdn, posInCodon);
		this.rawRefCodon = refCdn.toUpperCase();
		this.rawAltCodon = altCdn.toUpperCase();
		this.cdnaPosition = bvt.cdnaStart();
		this.cdnaEnd = bvt.cdnaEnd();
		this.cdsSequence = cdsSeq;

		if (cdsPos <= 3 && !transcript.isCdsStartNF() && CodonTable.isStart(refCdn) && !CodonTable.isStart(altCdn)) {
			this.consequence = "start_lost";
		} else if (rAA == '*' && aAA == '*') {
			this.consequence = "stop_retained_variant";
		} else if (rAA == '*' && aAA != '*') {
			this.consequence = "stop_lost";
		} else if (aAA == '*') {
			this.consequence = "stop_gained";
		} else if (rAA == aAA) {
			this.consequence = "synonymous_variant";
		} else {
			this.consequence = "missense_variant";
		}

		// Build alt CDS+UTR for SNP stop_lost/frameshift — matches Perl _get_alternate_cds.
		// VEP: upstream + alt_allele + downstream + 3'UTR
		if ("stop_lost".equals(this.consequence) || "frameshift_variant".equals(this.consequence)) {
			char[] altCdsChars = cdsSeq.toCharArray();
			altCdsChars[cdsPos - 1] = effectiveAlt.charAt(0);
			String altCds = new String(altCdsChars);
			String utr3 = BaseTranscriptVariation.threePrimeUtr(transcript, reference);
			this.altCdsSequence = altCds + (utr3 != null ? utr3 : "");
		}

		// Build HgvsNotation for SNP — matches Perl hgvs_protein() lines 1689-1735.
		// VEP guard (line 1657-1664): no HGVSp unless coding AND translation_start AND translation_end.
		if (bvt.translationStart() > 0 && bvt.translationEnd() > 0) {
			HgvsNotation n = new HgvsNotation();
			n.start = bvt.translationStart();
			n.end = bvt.translationStart();
			// translate * to X (Perl line 1987-1988)
			n.ref = String.valueOf(rAA == '*' ? 'X' : rAA);
			n.alt = String.valueOf(aAA == '*' ? 'X' : aAA);
			// VEP _get_hgvs_protein_type (line 1996-1997): both length 1 → ">"
			n.type = ">";
			this.hgvsNotation = n;
			this.cdsSequence = cdsSeq;
		}
	}

	private void annotateIndel(TranscriptModel transcript, String chr, int variantStart, int variantEnd,
			String vepAllele, String refAllele, boolean isDeletion) {

		if (bvt.cdsStart() < 0 && bvt.cdsEnd() < 0) return;
		BaseTranscriptVariation tv = bvt;

		int cdsStart = tv.cdsStart();
		int cdsEnd = tv.cdsEnd();

		String cdsSequence = BaseTranscriptVariation.translateableSeq(transcript, reference);
		if (cdsSequence == null) return;

		// VEP: vf_nt_len = cds_end - cds_start + 1 (ref CDS span)
		// VEP: allele_len = length(alt allele) (0 for pure deletions)
		int vfNtLen; // ref CDS span
		int alleleLen; // alt allele length
		if (isDeletion) {
			vfNtLen = Math.abs(cdsEnd - cdsStart) + 1;
			if (vfNtLen <= 0) return;
			alleleLen = "-".equals(vepAllele) ? 0 : vepAllele.length();
		} else {
			vfNtLen = "-".equals(refAllele) ? 0 : Math.abs(cdsEnd - cdsStart) + 1;
			alleleLen = vepAllele.length();
		}
		// indelLength = the ref span for deletions, alt length for insertions
		// Used for CDS position calculations and codon extraction
		int indelLength = isDeletion ? vfNtLen : alleleLen;

		// Use BaseTranscriptVariation coordinates for all position fields
		int cdsPos = cdsStart;
		this.cdsPosition = tv.cdsStart();
		this.cdsEnd = tv.cdsEnd();
		this.proteinPosition = tv.translationStart();
		this.cdnaPosition = tv.cdnaStart();
		this.cdnaEnd = tv.cdnaEnd();
		if (!isDeletion) {
			// Also store cdna end from BaseTranscriptVariation
			this.cdnaEnd = tv.cdnaEnd();
		}

		// VEP partial_codon guard (VariationEffect.pm line 1389-1414):
		// Checked BEFORE frameshift/inframe — blocks those if variant is in incomplete terminal codon.
		// VEP checks translation_start is defined (must map to CDS) and variant falls
		// entirely within the last incomplete codon. For deletions spanning past CDS end,
		// the fallback path handles stop_lost etc.
		if (isPartialCodon(transcript, cdsPos, cdsSequence.length())
				&& (!isDeletion || cdsEnd <= cdsSequence.length())) {
			this.consequence = ("incomplete_terminal_codon_variant");
			return;
		}

		// VEP frameshift check (VariationEffect.pm line 1346-1387):
		// abs(allele_len - vf_nt_len) % 3 != 0
		boolean isFrameshift = Math.abs(alleleLen - vfNtLen) % 3 != 0;
		Trace.log("annotateIndel.entry", "tr=%s cdsPos=%d cdsEnd=%d alleleLen=%d vfNtLen=%d isFrameshift=%b cdsLen=%d",
			transcript.getTranscriptId(), cdsPos, this.cdsEnd, alleleLen, vfNtLen, isFrameshift, cdsSequence.length());

		// Get the affected codon region peptides (like VEP's _get_peptide_alleles)
		int codonStart = ((cdsPos - 1) / 3) * 3;
		String refCodonRegion = safeSubstring(cdsSequence, codonStart, codonStart + 3);
		String refLocalPep = refCodonRegion != null ? String.valueOf(CodonTable.translate(refCodonRegion)) : null;

		// Check if variant overlaps stop codon (VEP: _overlaps_stop_codon, line 1358-1380)
		// Must check the full extent of the variant, not just the start position
		int cdsLen = cdsSequence.length();
		int stopCodonStart = cdsLen - 2; // 1-based: last 3 positions
		// VEP overlap formula (Utils.pm): overlap(a, b, c, d) = (b >= c) && (a <= d)
		// where a=cds_start, b=cds_end, c=feat_start, d=feat_end. Works correctly for
		// pure insertions (cds_start > cds_end) AND for deletions/delins (cds_start <= cds_end).
		boolean overlapsStop = cdsEnd >= stopCodonStart && cdsStart <= cdsLen;

		// Check if variant overlaps start codon (VEP: _overlaps_start_codon, line 965-986)
		// Start codon = CDS positions 1-3. VEP guards: return 0 if cds_start_NF (line 959)
		boolean overlapsStart = !transcript.isCdsStartNF() && cdsEnd >= 1 && cdsStart <= 3;
		int varCdsStart = cdsStart;
		int varCdsEnd = Math.max(cdsStart, cdsEnd);

		// Apply indel and get local alt peptide
		// VEP _get_alternate_cds appends 3'UTR so reading frame can extend into UTR for frameshifts
		String utr3 = BaseTranscriptVariation.threePrimeUtr(transcript, reference);
		String altCdsWithUtr = applyIndelToCds(cdsSequence, cdsPos, vepAllele, refAllele, isDeletion, transcript, indelLength, utr3);
		// Also keep a CDS-only version for position-sensitive checks
		String altCds = applyIndelToCds(cdsSequence, cdsPos, vepAllele, refAllele, isDeletion, transcript, indelLength);

		// VEP codon() line 837-848: for the REF allele when allele_len != vf_nt_len
		// (variant spans introns), Perl calls _get_alternate_cds with the REF allele
		// (genomic sequence including intron bases), producing a chimeric CDS.
		// This ref CDS is used for both codon display and consequence predicates.
		int refAlleleLen = "-".equals(refAllele) ? 0 : refAllele.length();
		String refCds = cdsSequence; // default: use original CDS
		if (isDeletion && refAlleleLen != vfNtLen && refAlleleLen > 0) {
			// Multi-exon deletion: ref allele includes intron bases → splice ref into CDS
			// (Perl codon() line 837-848, _get_alternate_cds with REF allele)
			String refAlleleSeq = transcript.isPositiveStrand() ? refAllele
				: Sequence.reverseComplement(refAllele);
			refCds = safeSubstring(cdsSequence, 0, cdsPos - 1)
				+ refAlleleSeq
				+ safeSubstring(cdsSequence, this.cdsEnd, cdsSequence.length());
		} else if (refAlleleLen == vfNtLen && refAlleleLen > 0) {
			// Same-length (allele_len == vf_nt_len) — applies to ALL variant types:
			// deletions, MNPs, delins. Perl codon() line 854-859 always splices the
			// REF allele into _translateable_seq at cds_start-1 in this case. Normally
			// a no-op (raw CDS at that position already equals ref), but when the
			// transcript has a phase mismatch between start_Exon and
			// translation->start_Exon (e.g. FBtr0079971), cds_start is off by one
			// and the splice overwrites a different raw byte. Java must replicate that
			// splice so the displayed ref codon matches Perl.
			String refAlleleSeq = transcript.isPositiveStrand() ? refAllele
				: Sequence.reverseComplement(refAllele);
			refCds = safeSubstring(cdsSequence, 0, cdsPos - 1)
				+ refAlleleSeq
				+ safeSubstring(cdsSequence, cdsPos - 1 + refAlleleLen, cdsSequence.length());
		}

		// === VEP hgvs_protein() lines 1686-1741: exact method port ===

		// VEP codon() line 805, 818-820: uses tv->translation_start/end
		int translationStart = tv.translationStart();
		int translationEnd = tv.translationEnd();
		// VEP line 818-820: codon boundaries
		// codon_cds_start = tv_tr_start * 3 - 2 = (translationStart - 1) * 3 (0-based)
		// codon_cds_end = tv_tr_end * 3 = translationEnd * 3 - 1 (0-based inclusive)
		int codonCdsStart0 = (translationStart - 1) * 3;
		int codonCdsEnd0 = translationEnd * 3 - 1;
		// VEP line 820: codon_len = codon_cds_end - codon_cds_start + 1
		int codonLen0 = codonCdsEnd0 - codonCdsStart0 + 1;
		// VEP line 828
		int altCodonLen0 = codonLen0 + (alleleLen - vfNtLen);

		Trace.log("annotateIndel.preHgvsp", "tr=%s refLocalPep=%s altCdsWithUtr=%s altCds=%s codonLen0=%d",
			transcript.getTranscriptId(),
			refLocalPep != null ? refLocalPep : "null",
			altCdsWithUtr != null ? "len=" + altCdsWithUtr.length() : "null",
			altCds != null ? "len=" + altCds.length() : "null",
			codonLen0);
		if (refLocalPep != null && refLocalPep.length() > 0 && altCdsWithUtr != null) {
			// VEP codon() line 856-859: REF codon comes from the CDS with the REF allele
			// SPLICED IN (Perl does this in the allele_len == vf_nt_len branch and via
			// _get_alternate_cds in the allele_len != vf_nt_len branch). Using refCds here
			// (built earlier with the ref allele spliced for same-length substitutions,
			// otherwise equal to cdsSequence) ensures HGVSp sees the same ref peptide as
			// the codon display path — previously this used raw cdsSequence, which gave
			// "SA" instead of "RP" for MNPs on transcripts with a phase off-by-one.
			String refCodonStr = vepCodon(refCds, codonCdsStart0, codonLen0);
			String altCodonStr = vepCodon(altCds, codonCdsStart0, Math.max(0, altCodonLen0));
			Trace.log("TVA.codon_extract", "tr=%s codonCdsStart0=%d codonLen0=%d alleleLen=%d vfNtLen=%d cds_len=%d",
				transcript.getTranscriptId(), codonCdsStart0, codonLen0, alleleLen, vfNtLen, cdsSequence.length());

			// VEP peptide() line 684-778: translate codon to SHORT peptide
			String shortRefPep = vepPeptide(refCodonStr);
			String shortAltPep = vepPeptide(altCodonStr);

			// VEP line 1720-1726: guard — must have ref peptide. Perl only skips
			// _clip_alleles when ref==alt (line 1729), but still runs _get_hgvs_protein_type
			// and _get_hgvs_peptides, which via _get_fs_peptides detects stop_retained
			// frameshift insertions (ref='*' alt='*') and emits p.TerNNN=.
			if (refLocalPep != null && refLocalPep.length() > 0 && altCdsWithUtr != null) {

				// VEP _clip_alleles (line 1729) — only called when ref != alt in Perl.
				// When ref==alt, skip clip and start with an empty notation at
				// translation_start/end. _get_hgvs_protein_type will still route through
				// the frameshift branch (→ _get_fs_peptides) if this is a frameshift.
				HgvsNotation n;
				if (shortRefPep.equals(shortAltPep)) {
					n = new HgvsNotation();
					n.ref = shortRefPep;
					n.alt = shortAltPep;
					n.start = translationStart;
					n.end = translationEnd;
					n.preseq = "";
				} else {
					n = vepClipAlleles(shortRefPep, shortAltPep,
						translationStart, translationEnd);
				}

				// VEP _get_hgvs_protein_type (line 1729).
				// Perl mutates $hgvs_notation->{type} in-place — it OVERRIDES whatever
				// _clip_alleles set (e.g., clip sees an "ins with matching preseq" and
				// sets type=dup, but protein_type sees ref="" and resets type=ins).
				// Mirror that: write hgvsType back into n.type so downstream checks see
				// the protein_type view, not the clip view.
				String hgvsType = vepGetHgvsProteinType(n, isFrameshift);
				n.type = hgvsType;

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
				boolean insNoSurrounding = false;
				if ("ins".equals(hgvsType) && noStop != null) {
					// Line 2041: _check_peptides_post_var → _shift_3prime
					vepShift3Prime(n, "ins", noStop);

					// Line 2044: _check_for_peptide_duplication
					if (!n.alt.contains("*")) {
						vepCheckForPeptideDuplication(n, noStop);
					}
					if ("dup".equals(n.type)) hgvsType = "dup";

					// Line 2047-2060: set ref to surrounding peptides for ins notation.
					// VEP _get_surrounding_peptides returns undef when min >= len(peptide)
					// (where peptide excludes the stop codon). In that case, the whole
					// _get_hgvs_peptides returns undef (line 2077) and hgvs_protein returns
					// undef with reason=no_peptides. C-terminal insertions (insertion
					// between last coding AA and stop) hit this path.
					if ("ins".equals(hgvsType)) {
						int minPos = Math.min(n.start, n.end);
						if (minPos >= 1 && minPos + 1 <= noStop.length()) {
							// 2 chars from min
							String surr = noStop.substring(minPos - 1, Math.min(minPos + 1, noStop.length()));
							if (surr.length() == 2) {
								n.ref = surr;
							} else {
								insNoSurrounding = true;
							}
						} else {
							insNoSurrounding = true;
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
				if (this.consequence != null && this.consequence.contains("start_lost")) {
					n.alt = "?";
					n.type = "";
				}

				// Set HGVSp results from notation
				this.hgvsProteinPosition = (n.start);
				this.hgvsProteinEnd = (n.end);
				this.clippedRefPeptide = (n.ref);
				this.clippedAltPeptide = (n.alt);
				this.hgvsType = (hgvsType);

				// ref/alt AA at first differing position
				if ("fs".equals(hgvsType) && n.ref != null && n.ref.length() == 1
						&& n.alt != null && n.alt.length() == 1) {
					// _get_fs_peptides set ref/alt to the first differing AA
					this.refAA = (n.ref.charAt(0));
					this.altAA = (n.alt.charAt(0));
				} else {
					int prefixLen = n.preseq != null ? n.preseq.length() : 0;
					if (prefixLen < shortRefPep.length()) {
						this.refAA = (shortRefPep.charAt(prefixLen));
					} else if (!shortRefPep.isEmpty()) {
						this.refAA = (shortRefPep.charAt(0));
					}
					if (prefixLen < shortAltPep.length()) {
						this.altAA = (shortAltPep.charAt(prefixLen));
					}
				}

				// Flanking AAs for insertion HGVSp (VEP _get_surrounding_peptides)
				if ("ins".equals(hgvsType) || "dup".equals(hgvsType)) {
					if (fullRefPep != null) {
						int insProtPos = "dup".equals(hgvsType) ? n.end + 1 : n.start;
						if (insProtPos >= 2 && insProtPos <= fullRefPep.length()) {
							this.flankLeftAA = (fullRefPep.charAt(insProtPos - 2));
							this.flankRightAA = (fullRefPep.charAt(insProtPos - 1));
						}
					}
				}
				// Store notation + CDS sequences for _get_hgvs_protein_format.
				// Skip when Perl's _get_surrounding_peptides would have returned undef
				// (C-terminal insertions — see insNoSurrounding guard above).
				if (!insNoSurrounding) {
					n.type = hgvsType;
					this.hgvsNotation = (n);
					this.cdsSequence = cdsSequence;
					// Perl _get_alternate_cds always appends 3'UTR
					this.altCdsSequence = altCdsWithUtr != null ? altCdsWithUtr : altCds;
				}
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

			// VEP stop_gained (VariationEffect.pm line 1146-1166): fires when alt peptide
			// contains '*' AND ref peptide does not. Perl's peptide() for a frameshift
			// extracts codon_len + (allele_len - vf_nt_len) bases from the alt CDS starting
			// at codon_cds_start (= (tv_tr_start-1)*3), translates whole codons, and appends
			// 'X' for any partial trailing codon (TranscriptVariationAllele.pm line 684-778).
			// If the resulting alt peptide contains '*' while the ref peptide does not,
			// stop_gained fires alongside frameshift_variant.
			// VEP stop_gained (VariationEffect.pm line 1146-1166):
			// alt_pep contains '*' AND ref_pep doesn't.
			// VEP's _get_peptide_alleles returns codon/peptide from $bvfoa->peptide().
			// For between-codon insertions (codonLen0 <= 0): ref="-", alt=translated insertion.
			if (altCds != null && codonCdsStart0 >= 0) {
				String refPep;
				String altPep;
				if (codonLen0 > 0) {
					int altExtractLen = codonLen0 + (alleleLen - vfNtLen);
					String altCodonStr = altExtractLen > 0 ? vepCodon(altCds, codonCdsStart0, altExtractLen) : null;
					altPep = altCodonStr != null ? vepPeptide(altCodonStr) : null;
					String refCodonStr = vepCodon(cdsSequence, codonCdsStart0, codonLen0);
					refPep = refCodonStr != null ? vepPeptide(refCodonStr) : null;
				} else {
					// Between-codon insertion: Perl peptide() returns "-" for ref, translated insertion for alt
					refPep = "-";
					String insSeq = transcript.isPositiveStrand() ? vepAllele : Sequence.reverseComplement(vepAllele);
					if ("-".equals(insSeq)) insSeq = "";
					altPep = insSeq.isEmpty() ? "-" : vepPeptide(insSeq);
				}
				if (altPep != null && altPep.contains("*")
						&& (refPep == null || !refPep.contains("*"))) {
					consequences.add("stop_gained");
				}
			}
		} else {
			// In-frame indel
			// Get local codon alleles matching VEP's _get_codon_alleles logic
			// (TranscriptVariationAllele.pm line 808: tv_tr_start = translation_start, tv_tr_end = translation_end)
			// For insertions, VEP convention: cds_start > cds_end → translation_start > translation_end
			//	 → codon_len = end-start+1 may be negative or zero
			int protStart = (cdsStart - 1) / 3 + 1;
			int protEnd = (cdsEnd - 1) / 3 + 1;
			int codonCdsStart = protStart * 3 - 2;
			int codonCdsEnd = protEnd * 3;
			int codonLen = codonCdsEnd - codonCdsStart + 1;

			// VEP codon() line 837-848: when allele_len != vf_nt_len, Perl builds CDS
			// via _get_alternate_cds with the allele. Use refCds for ref codon extraction
			// (matches Perl's behavior for multi-exon variants where genomic ref includes introns).
			int refCodonExtractLen = codonLen + (refAlleleLen > 0 ? refAlleleLen - vfNtLen : 0);
			String refCodon = safeSubstring(refCds, codonCdsStart - 1, codonCdsStart - 1 + refCodonExtractLen);
			// VEP line 828: codon_len + (allele_len - vf_nt_len). Works for ALL three cases:
			//	 insertion (alleleLen > vfNtLen)   → altCodonLen > codonLen
			//	 deletion  (alleleLen < vfNtLen)   → altCodonLen < codonLen
			//	 MNP	   (alleleLen == vfNtLen)  → altCodonLen == codonLen (no change)
			// Previously used codonLen ± indelLength which double-counted for same-length MNPs
			// (altCodonLen came out 6+3=9 instead of 6, producing a 3-AA altPep from a 2-AA
			// ref codon region and forcing protein_altering_variant instead of missense_variant).
			int altCodonLen = codonLen + (alleleLen - vfNtLen);
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
				// VEP stop_lost (VariationEffect.pm line 1190-1193):
				// Uses _get_peptide_alleles (local peptides from codon()):
				// ($alt_pep !~ /\*/) and ($ref_pep =~ /\*/)
				boolean localRefHasStop = refPep != null && refPep.contains("*");
				boolean localAltHasNoStop = altPep == null || altPep.isEmpty() || !altPep.contains("*");
				Trace.log("del.stop_lost_check", "tr=%s localRefHasStop=%b localAltHasNoStop=%b overlapsStop=%b refHasStop=%b refPepLen=%d altPepLen=%d refPep_first10=%s",
					transcript.getTranscriptId(), localRefHasStop, localAltHasNoStop, overlapsStop, refHasStop,
					refPep != null ? refPep.length() : 0, altPep != null ? altPep.length() : 0,
					refPep != null && refPep.length() > 0 ? refPep.substring(0, Math.min(10, refPep.length())) : "null");
				if (localRefHasStop && localAltHasNoStop) {
					consequences.add("stop_lost");
					// VEP inframe_deletion: checks codon pattern (ref starts/ends with alt).
					// If pattern fails → protein_altering_variant instead.
					boolean isInframeDel = false;
					if (refCodon != null && altCodon != null && altCodon.length() < refCodon.length()) {
						isInframeDel = refCodon.startsWith(altCodon) || refCodon.endsWith(altCodon);
						if (!isInframeDel) {
							// Perl trim_sequences check: trim common prefix+suffix, check if alt is empty
							String[] trimmed = trimSequences(refCodon, altCodon);
							isInframeDel = trimmed[1].isEmpty() && trimmed[0].length() % 3 == 0;
						}
					}
					consequences.add(isInframeDel ? "inframe_deletion" : "protein_altering_variant");
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
						}
					}

					// VEP missense_variant predicate (VariationEffect.pm):
					// fires when ref_pep and alt_pep are non-empty, same length, and differ.
					// For same-length multi-base substitutions (MNPs, delins) this is the
					// correct classification — inframe_insertion/inframe_deletion require
					// an actual length change in the peptide.
					boolean isMissense = refPep != null && altPep != null
						&& refPep.length() == altPep.length()
						&& refPep.length() > 0
						&& !refPep.equals(altPep)
						&& !refPep.contains("*") && !altPep.contains("*");

					if (isMissense) {
						// Same-length peptide substitution (e.g. MNP spanning multiple codons,
						// or a delins where both sides happen to produce equal-length peptides).
						// VEP inframe_insertion / inframe_deletion predicates require a length
						// change, so only missense_variant applies here.
						consequences.add("missense_variant");
					} else if (isProteinAltering) {
						consequences.add("protein_altering_variant");
					} else if (refPep != null && altPep != null
						&& refPep.length() == altPep.length()
						&& refPep.equals(altPep)) {
						// Synonymous multi-base: both peptides equal.
						consequences.add("synonymous_variant");
					} else {
						consequences.add("inframe_insertion");
						if (overlapsStop && refHasStop && altPep != null && altPep.contains("*")) {
							consequences.add("stop_retained_variant");
						}
					}
					// VEP stop_lost for insertions (VariationEffect.pm line 1168-1221):
					// ($alt_pep !~ /\*/) and ($ref_pep =~ /\*/)
					// Uses RAW alt peptide — if alt still has '*', stop is retained, not lost.
					// When stop_lost fires, remove inframe_insertion (Perl's inframe_insertion
					// predicate fails when ref is stop and alt doesn't contain ref).
					if (overlapsStop && refPep != null && refPep.contains("*")
							&& (altPep == null || !altPep.contains("*"))) {
						consequences.add("stop_lost");
						consequences.remove("inframe_insertion");
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
				// Use refCds (built earlier via _get_alternate_cds with REF allele)
				// for ref codon extraction — matches Perl's codon() line 837-848.
				int refCodonExtractLen2 = codonLen0 + (refAlleleLen > 0 && refAlleleLen != vfNtLen ? refAlleleLen - vfNtLen : 0);
				rc = safeSubstring(refCds, codonCdsStart0, codonCdsStart0 + refCodonExtractLen2);
				ac = altCds != null ? safeSubstring(altCds, codonCdsStart0, codonCdsStart0 + Math.max(0, altCodonLen0)) : null;
				rp = rc != null ? translateCds(rc) : null;
				ap = ac != null && ac.length() > 0 ? translateCds(ac) : "-";
			}

			// VEP pep_allele_string (line 610-622)
			if (rp != null) {
				String rpStr = rp.isEmpty() ? "-" : rp;
				String apStr = (ap == null || ap.isEmpty()) ? "-" : ap;
				this.aminoAcids = (pepAlleleString(rpStr, apStr));
			}

			// VEP display_codon (line 884-915) + display_codon_allele_string (line 658-673)
			// VEP codon_position from BaseTranscriptVariation (line 287-307)
			int codonPosition1 = tv.codonPosition();
			// VEP feature_seq: ref TVA gets refAllele, alt TVA gets vepAllele
			// For deletions: ref feature_seq = deleted bases, alt feature_seq = "-"
			// For insertions: ref feature_seq = "-", alt feature_seq = inserted bases
			String refFeatureSeq = "-".equals(refAllele) ? "-" : refAllele;
			String altFeatureSeq = "-".equals(vepAllele) ? "-" : vepAllele;
			String refDisplay = displayCodon(rc, refFeatureSeq, codonPosition1);
			String altDisplay = displayCodon(ac, altFeatureSeq, codonPosition1);
			if (refDisplay == null) refDisplay = "-";
			if (altDisplay == null) altDisplay = "-";
			this.codons = (displayCodonAlleleString(refDisplay, altDisplay));
		}

		// Sort by VEP rank (most severe first) to match VEP output order
		consequences.sort((a, b) -> Integer.compare(
			ConsequenceSeverity.getRank(a), ConsequenceSeverity.getRank(b)));
		this.consequence = (String.join("&", consequences));
		Trace.log("annotateIndel.consequence", "tr=%s consequence=%s size=%d",
			transcript.getTranscriptId(), this.consequence, consequences.size());

		// Compute fsTer/extTer count (VEP _stop_loss_extra_AA, line 2386-2435)
		if (altCdsWithUtr != null) {
			if (consequences.contains("frameshift_variant") && !consequences.contains("stop_gained")) {
				this.fsTerCount = (computeTerCount(altCdsWithUtr, hgvsProteinPosition > 0 ? hgvsProteinPosition : proteinPosition, cdsSequence.length()));
			}
			if (consequences.contains("stop_lost")) {
				int origStopProtPos = cdsSequence.length() / 3;
				this.extTerCount = (computeExtTerCount(altCdsWithUtr, origStopProtPos));
			}
		}

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

	// formatDisplayCodon removed — replaced by displayCodon() Perl port

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
		// VEP peptide() line 766-768: partial trailing codon → append 'X' unless
		// $pep eq '*' (whole string equals '*', not just ends with it). So peptide
		// "C*" with a partial codon still gets X → "C*X".
		if (cds.length() % 3 != 0 && !pep.toString().equals("*")) {
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
			String cds = BaseTranscriptVariation.translateableSeq(transcript, reference);
			if (cds == null || cds.length() < 3) return false;
			if (!CodonTable.isStop(cds.substring(cds.length() - 3))) return false;

			// 3. Build 3' UTR sequence
			String utr3 = BaseTranscriptVariation.threePrimeUtr(transcript, reference);
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
			Trace.log("SILENT_CATCH_2", "error=%s at %s", e.toString(), e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "?");
			return false;
		}
	}

	/**
	 * Find the CDS position of the first CDS base within a genomic range.
	 * VEP's genomic2cds maps the entire range and takes the first Coordinate.
	 * The variant start may be in UTR/intron but the range still overlaps CDS.
	 */
	private int findFirstCdsPositionInRange(TranscriptModel transcript, int variantStart, int variantEnd) {
		// VEP uses genomic2cds which maps the range and takes the first Coordinate
		TranscriptMapper mapper = new TranscriptMapper(transcript);
		int strand = transcript.isPositiveStrand() ? 1 : -1;
		List<Mapper.Result> cdsCoords = mapper.genomic2cds(variantStart, variantEnd, strand);

		// Find first non-gap result
		int exonPhase = transcript.getStartExonPhase();
		int phaseOffset = exonPhase > 0 ? exonPhase : 0;
		for (Mapper.Result r : cdsCoords) {
			if (r.isCoordinate()) {
				return r.coordinate.start + phaseOffset;
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
			String cds = BaseTranscriptVariation.translateableSeq(transcript, reference);
			if (cds == null || cds.length() < 3) return false;

			// 3. Build 5' UTR sequence and concatenate
			String utr5 = BaseTranscriptVariation.fivePrimeUtr(transcript, reference);
			String utrAndCds = utr5 + cds;

			// 4. Compute cDNA position via BaseTranscriptVariation.genomicToCdna
			int cdnaStart = BaseTranscriptVariation.genomicToCdna(transcript, variantStart);
			if (cdnaStart < 0) {
				// Variant start might be in intron — find first exonic position in range
				for (ExonModel exon : transcript.getExons()) {
					int firstExonic = transcript.isPositiveStrand()
						? Math.max(variantStart, exon.getStart())
						: Math.min(variantEnd, exon.getEnd());
					if (firstExonic >= exon.getStart() && firstExonic <= exon.getEnd()) {
						cdnaStart = BaseTranscriptVariation.genomicToCdna(transcript, firstExonic);
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
			Trace.log("SILENT_CATCH_3", "error=%s at %s", e.toString(), e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "?");
			return false;
		}
	}

	/** Build the 5' UTR sequence from FASTA for this transcript. */
	// build5PrimeUtr removed — replaced by BaseTranscriptVariation.fivePrimeUtr()
	// build3PrimeUtr removed — replaced by BaseTranscriptVariation.threePrimeUtr()

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
			String cds = BaseTranscriptVariation.translateableSeq(transcript, reference);
			if (cds == null || cds.length() < 3) return false;
			if (!CodonTable.isStop(cds.substring(cds.length() - 3))) return false;
			return !isStopAltered(transcript, chr, variantStart, variantEnd);
		} catch (Exception e) {
			Trace.log("SILENT_CATCH_4", "error=%s at %s", e.toString(), e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "?");
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
			// VEP _get_alternate_cds (line 2347-2360):
			//	 upstream	= substr(reference_cds_seq, 0, cds_start - 1)
			//	 downstream = substr(reference_cds_seq, cds_end)
			//	 alternate	= upstream + alt_allele + downstream
			// cdsPos is cds_start (1-based). this.cdsEnd is cds_end (1-based).
			// - Pure insertion:	  cds_start > cds_end → downstream starts at cds_end = cds_start-1
			//						  → no ref bases are replaced.
			// - Pure/net deletion:	  cds_start <= cds_end, alt empty or shorter.
			// - Delins (net ins):	  cds_start <= cds_end, alt longer than ref region.
			int upEnd = Math.max(0, Math.min(cdsPos - 1, seq.length()));
			int downStart = Math.max(upEnd, Math.min(this.cdsEnd, seq.length()));
			String replaceSeq = "-".equals(vepAllele) ? "" :
				(transcript.isPositiveStrand() ? vepAllele : Sequence.reverseComplement(vepAllele));
			return seq.substring(0, upEnd) + replaceSeq + seq.substring(downStart);
		} catch (Exception e) {
			Trace.log("SILENT_CATCH_5", "error=%s at %s", e.toString(), e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "?");
			return null;
		}
	}

	// genomicToCdsPosition removed — replaced by BaseTranscriptVariation.genomicToCds()

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

	// buildCdsSequence removed — replaced by BaseTranscriptVariation.translateableSeq()

	// computeCdnaPosition removed — replaced by BaseTranscriptVariation.genomicToCdna()

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
	 * Detects dup/ins/del/>/delins type from clipped this.
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
			seqToCheck = n.alt;		// Line 2511
		} else if ("del".equals(hgvsType)) {
			seqToCheck = n.ref;		// Line 2514
		} else {
			return;					 // Line 2516-2518
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
				n.end = n.start - 1;			  // Line 2375
				n.start -= n.alt.length();			// Line 2376
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
		Trace.log("vepStopLossExtraAA.entry", "test=%s refVarPos=%d altCds=%s refPep=%s",
			test, refVarPos, altCds != null ? "len=" + altCds.length() : "null", refPep != null ? "len=" + refPep.length() : "null");
		if (refVarPos <= 0) return null;
		if (altCds == null) return null;

		// Line 2401: translate alt CDS (BioPerl translate — whole codons only)
		String altTrans = translateCdsWholeOnly(altCds);
		// Line 2403-2404: ref length — Perl _peptide() excludes trailing stop codon
		// Ensembl Transcript::translate removes the final stop codon before translating.
		int refLen = (refPep != null) ? refPep.length() : 0;
		if (refPep != null && refLen > 0 && refPep.charAt(refLen - 1) == '*') refLen--;
		// Line 2412: find stop in alt translation
		int stopPos = altTrans.indexOf('*');
		Trace.log("vepStopLossExtraAA", "test=%s refVarPos=%d refLen=%d stopPos=%d altTransLen=%d altCdsLen=%d",
			test, refVarPos, refLen, stopPos, altTrans.length(), altCds.length());
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

		// AGR production pipeline (RunVep.pm) uses --remove_hgvsp_version
		// so HGVSp does NOT include the .{version} suffix on protein IDs.
		String prefix = (proteinId != null ? proteinId : "") + ":p.";

		// Convert ref/alt to 3-letter code (VEP line 2067-2071)
		String ref3 = to3Letter(n.ref);
		String alt3 = to3Letter(n.alt);

		// VEP line 2072: alt = "del" if alt == "-"
		if ("-".equals(n.alt) || n.alt.isEmpty()) alt3 = "del";

		// VEP line 2075-2078: start_lost sets alt="?" and type="".
		// Then _get_hgvs_protein_format line 1959-1960 formats as
		// "ref + start + '_' + alt + end" when start != end (multi-AA ref),
		// otherwise "ref + start + alt" (single-AA ref).
		if (isStartLost) {
			alt3 = "?";
			if (n.start != n.end) {
				return prefix + ref3 + n.start + "_" + alt3 + n.end;
			}
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
	public static int vepCodonPosition(int cdnaStart, TranscriptModel transcript) {
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
			char c = oneLetterPep.charAt(i);
			// VEP _get_hgvs_protein_type (line 1987-1988) replaces '*' with 'X' before 3-letter conversion,
			// then VEP _get_hgvs_peptides (line 2114-2115) substitutes 'Xaa' → 'Ter' as the recommended
			// HGVS stop notation. Emit Ter directly for '*' / 'X' to match.
			if (c == '*' || c == 'X') {
				sb.append("Ter");
			} else {
				sb.append(AminoAcid.threeLetterCode(c));
			}
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

	// ===================================================================
	// VEP hgvs_transcript() — ported from TranscriptVariationAllele.pm line 1281-1485
	// Previously in HgvsCodingNotation/HgvsTranscript as a separate class.
	// ===================================================================

	/**
	 * VEP hgvs_transcript() — generates HGVSc notation.
	 * Matches TranscriptVariationAllele.pm lines 1294-1486.
	 */
	public String hgvsTranscript(TranscriptModel transcript, String chr, int variantStart, int variantEnd,
			String vepAllele, String refAllele, int cdsPosition, boolean isCoding) {

		// VEP TranscriptVariationAllele.pm line 1316: skip alleles with non-ACGT characters
		String checkSeq = vepAllele != null ? vepAllele.replace("-", "") : "";
		if (!checkSeq.isEmpty() && !checkSeq.matches("[ACGTacgt]+")) return null;

		String prefix = isCoding ? "c." : "n.";
		String transcriptRef = transcript.getTranscriptId();
		if (transcriptRef == null) return null;
		Trace.log("hgvsTranscript.entry", "tr=%s varStart=%d varEnd=%d vepAllele=%s refAllele=%s cdsPos=%d",
			transcriptRef, variantStart, variantEnd, vepAllele, refAllele, cdsPosition);

		// VEP line 1425-1426: append version
		if (!transcriptRef.matches(".*\\.\\d+$")) {
			transcriptRef = transcriptRef + ".1";
		}

		// VEP line 1446-1448: for SNPs in exons, use cds_start directly (avoids _get_cDNA_position).
		// Perl: if var_class eq 'SNP' && exon && defined cds_start/end
		boolean isSNP = variantStart == variantEnd && refAllele.length() == 1 && vepAllele.length() == 1 && !"-".equals(refAllele);
		String startPos;
		String endPos;
		if (isSNP && isCoding && cdsPosition > 0) {
			startPos = String.valueOf(cdsPosition);
			endPos = startPos;
		} else {
			startPos = getCdnaPosition(transcript, variantStart, isCoding);
			endPos = getCdnaPosition(transcript, variantEnd, isCoding);
		}

		if (startPos == null && endPos == null) return null;
		// Perl returns undef if either position can't be mapped.
		// For deletions, both endpoints must be resolved to form a valid range.
		boolean isDeletion = "-".equals(vepAllele);
		if (isDeletion && (startPos == null || endPos == null)) return null;
		if (startPos == null) startPos = endPos;
		if (endPos == null) endPos = startPos;

		// VEP line 1456-1459: ensure ascending order
		if (compareHgvsPositions(startPos, endPos) > 0) {
			String tmp = startPos; startPos = endPos; endPos = tmp;
		}

		// Strand-aware ref/alt
		String hgvsRef = transcript.isPositiveStrand() ? refAllele : Sequence.reverseComplement(refAllele);
		String hgvsAlt = transcript.isPositiveStrand() ? vepAllele : Sequence.reverseComplement(vepAllele);

		String notation;
		if ("-".equals(vepAllele)) {
			// Deletion
			notation = startPos.equals(endPos) ? startPos + "del" : startPos + "_" + endPos + "del";
		} else if ("-".equals(refAllele)) {
			// Insertion: check for dup
			boolean isDup = false;
			int altLen = vepAllele.length();
			if (reference != null && chr != null) {
				try {
					int refEnd = Math.min(variantStart, variantEnd);
					int refStart = refEnd - altLen + 1;
					if (refStart >= 1) {
						String preceding = reference.getSequence(chr, refStart, refEnd);
						isDup = vepAllele.equalsIgnoreCase(preceding);
						Trace.log("hgvsTranscript.dup", "varStart=%d varEnd=%d refStart=%d refEnd=%d preceding=%s vepAllele=%s isDup=%b",
							variantStart, variantEnd, refStart, refEnd, preceding, vepAllele, isDup);
					}
				} catch (Exception e) {
					Trace.log("SILENT_CATCH_6", "error=%s at %s", e.toString(), e.getStackTrace().length > 0 ? e.getStackTrace()[0].toString() : "?");
				}
			}
			if (isDup) {
				int gEnd = Math.min(variantStart, variantEnd);
				int gStart = gEnd - altLen + 1;
				String dupStart = getCdnaPosition(transcript, gStart, isCoding);
				String dupEnd = getCdnaPosition(transcript, gEnd, isCoding);
				if (dupStart != null && dupEnd != null) {
					if (compareHgvsPositions(dupStart, dupEnd) > 0) {
						String tmp = dupStart; dupStart = dupEnd; dupEnd = tmp;
					}
					notation = dupStart.equals(dupEnd) ? dupStart + "dup" : dupStart + "_" + dupEnd + "dup";
				} else {
					notation = startPos + "_" + endPos + "ins" + hgvsAlt;
				}
			} else {
				notation = startPos + "_" + endPos + "ins" + hgvsAlt;
			}
		} else if (hgvsRef.length() == 1 && hgvsAlt.length() == 1) {
			notation = startPos + hgvsRef + ">" + hgvsAlt;
		} else if (hgvsRef.length() == hgvsAlt.length() && hgvsRef.length() > 1
				&& hgvsAlt.equalsIgnoreCase(Sequence.reverseComplement(hgvsRef))) {
			// Perl Utils/Sequence.pm line 557-563 (hgvs_variant_notation): when
			// allele lengths are equal and the alt is reverse-complement of the ref,
			// the type is 'inv'. Format per line 659-664 (format_hgvs_string):
			// coord[_coord] + 'inv' with no alt allele shown (when ref length > 1).
			// Perl does NOT run _clip_alleles for inv, so we skip it too.
			notation = startPos.equals(endPos)
				? startPos + "inv"
				: startPos + "_" + endPos + "inv";
		} else {
			// Complex: VEP _clip_alleles (line 1418)
			int clipStartInt = parseHgvsPos(startPos);
			int clipEndInt = parseHgvsPos(endPos);
			if (clipEndInt == 0) clipEndInt = clipStartInt;

			HgvsNotation clipped = vepClipAlleles(hgvsRef, hgvsAlt, clipStartInt, clipEndInt);
			Trace.log("hgvsTranscript.clip", "hgvsRef=%s hgvsAlt=%s startPos=%s endPos=%s type=%s clippedRef=%s clippedAlt=%s preseq=%s",
				hgvsRef, hgvsAlt, startPos, endPos, clipped.type, clipped.ref, clipped.alt,
				clipped.preseq != null ? clipped.preseq : "null");

			if (clipped.preseq != null && !clipped.preseq.isEmpty()) {
				int prefixLen = clipped.preseq.length();
				int newStartGenomic = transcript.isPositiveStrand()
					? variantStart + prefixLen : variantEnd - prefixLen;
				startPos = getCdnaPosition(transcript, newStartGenomic, isCoding);
				if (startPos == null) startPos = String.valueOf(clipped.start);
			}
			// VEP _clip_alleles also strips matching suffix bases — when checkEnd
			// was decremented, recompute endPos from the new genomic end position.
			if (clipped.end < clipEndInt) {
				int suffixLen = clipEndInt - clipped.end;
				int newEndGenomic = transcript.isPositiveStrand()
					? variantEnd - suffixLen : variantStart + suffixLen;
				String newEndPos = getCdnaPosition(transcript, newEndGenomic, isCoding);
				if (newEndPos != null) endPos = newEndPos;
				else endPos = String.valueOf(clipped.end);
			}

			String clippedRef = clipped.ref;
			String clippedAlt = clipped.alt;

			if ("=".equals(clipped.type)) {
				notation = startPos + "=";
			} else if ("dup".equals(clipped.type)) {
				// Perl _clip_alleles: start -= length(alt) for dups.
				// clipped.start/end are already adjusted CDS positions.
				String ds = String.valueOf(clipped.start);
				String de = String.valueOf(clipped.end);
				notation = ds.equals(de) ? ds + "dup" : ds + "_" + de + "dup";
			} else if ("ins".equals(clipped.type) || (clippedRef.isEmpty() && !clippedAlt.isEmpty())) {
				notation = endPos + "_" + startPos + "ins" + clippedAlt;
			} else if ("del".equals(clipped.type) || (!clippedRef.isEmpty() && clippedAlt.isEmpty())) {
				notation = startPos.equals(endPos) ? startPos + "del" : startPos + "_" + endPos + "del";
			} else if (">".equals(clipped.type) || (clippedRef.length() == 1 && clippedAlt.length() == 1)) {
				notation = startPos + clippedRef + ">" + clippedAlt;
			} else if ("dup".equals(clipped.type)) {
				// Perl _clip_alleles sets start -= length(alt) for dups.
				// Recompute startPos from the adjusted genomic coordinate.
				int dupLen = clippedAlt.length();
				int prefixLen2 = clipped.preseq != null ? clipped.preseq.length() : 0;
				int gDupStart = transcript.isPositiveStrand()
					? variantStart + prefixLen2 - dupLen
					: variantEnd - prefixLen2 + dupLen;
				String dupStartPos = getCdnaPosition(transcript, gDupStart, isCoding);
				if (dupStartPos == null) dupStartPos = startPos;
				// endPos is already at the last base of the prefix (= last duplicated base)
				if (compareHgvsPositions(dupStartPos, endPos) > 0) {
					String tmp = dupStartPos; dupStartPos = endPos; endPos = tmp;
				}
				notation = dupStartPos.equals(endPos) ? dupStartPos + "dup" : dupStartPos + "_" + endPos + "dup";
			} else {
				notation = startPos.equals(endPos)
					? startPos + "delins" + clippedAlt
					: startPos + "_" + endPos + "delins" + clippedAlt;
			}
		}

		return transcriptRef + ":" + prefix + notation;
	}

	/**
	 * VEP _get_cDNA_position (TranscriptVariationAllele.pm line 2662-2784).
	 */
	String getCdnaPosition(TranscriptModel transcript, int genomicPos, boolean isCoding) {
		java.util.List<ExonModel> exons = transcript.getExons();
		boolean positiveStrand = transcript.isPositiveStrand();

		int[] exCdnaStart = new int[exons.size()];
		int[] exCdnaEnd = new int[exons.size()];
		if (positiveStrand) {
			int running = 0;
			for (int i = 0; i < exons.size(); i++) {
				int len = exons.get(i).getEnd() - exons.get(i).getStart() + 1;
				exCdnaStart[i] = running + 1;
				exCdnaEnd[i] = running + len;
				running += len;
			}
		} else {
			int running = 0;
			for (int i = exons.size() - 1; i >= 0; i--) {
				int len = exons.get(i).getEnd() - exons.get(i).getStart() + 1;
				exCdnaStart[i] = running + 1;
				exCdnaEnd[i] = running + len;
				running += len;
			}
		}

		Integer cdnaPosition = null;
		String intronOffset = null;

		for (int i = 0; i < exons.size(); i++) {
			ExonModel exon = exons.get(i);
			if (genomicPos > exon.getEnd()) continue;
			if (genomicPos >= exon.getStart()) {
				cdnaPosition = exCdnaStart[i] + (
					positiveStrand ? (genomicPos - exon.getStart()) : (exon.getEnd() - genomicPos)
				);
				break;
			}
			if (i > 0) {
				ExonModel prevExon = exons.get(i - 1);
				int updist = Math.abs(genomicPos - prevExon.getEnd());
				int downdist = Math.abs(exon.getStart() - genomicPos);
				if (updist < downdist || (updist == downdist && positiveStrand)) {
					cdnaPosition = positiveStrand ? exCdnaEnd[i-1] : exCdnaStart[i-1];
					intronOffset = (positiveStrand ? "+" : "-") + updist;
				} else {
					cdnaPosition = positiveStrand ? exCdnaStart[i] : exCdnaEnd[i];
					intronOffset = (positiveStrand ? "-" : "+") + downdist;
				}
				break;
			}
			break;
		}

		if (cdnaPosition == null) return null;

		if (isCoding) {
			int cdnaCodingStart = transcript.getCdnaCodingStart();
			int cdnaCodingEnd = computeCdnaCodingEnd(transcript);

			if (cdnaCodingEnd > 0 && cdnaPosition > cdnaCodingEnd) {
				cdnaPosition -= cdnaCodingEnd;
				return "*" + cdnaPosition + (intronOffset != null ? intronOffset : "");
			}
			if (cdnaCodingEnd > 0 && cdnaPosition == cdnaCodingEnd && intronOffset != null) {
				return "*" + intronOffset.replace("+", "");
			}
			if (cdnaCodingStart > 0) {
				if (cdnaPosition >= cdnaCodingStart) cdnaPosition++;
				cdnaPosition -= cdnaCodingStart;
			}
		}

		return cdnaPosition + (intronOffset != null ? intronOffset : "");
	}

	/**
	 * Matches Perl Transcript.pm cdna_coding_end (line 1027-1074).
	 * Perl walks exons; when it hits translation->end_Exon, it adds
	 * translation->end (= CDS-end offset relative to exon start), even when
	 * that exceeds the exon length (e.g. FB FBtr0335486 where CDS extends 3bp
	 * past its exon). Java doesn't store translation end_Exon directly, so we
	 * identify the "end exon" as the last exon whose start overlaps the last
	 * CDS segment (matching Perl's assignment at BaseGXF.pm line 568).
	 */
	private int computeCdnaCodingEnd(TranscriptModel transcript) {
		if (!transcript.isCoding()) return 0;
		List<CdsSegment> cdsSegs = transcript.getCdsSegments();
		if (cdsSegs.isEmpty()) return 0;

		// Last CDS segment in genomic order: + strand → last, - strand → first
		CdsSegment lastCds = transcript.isPositiveStrand()
			? cdsSegs.get(cdsSegs.size() - 1) : cdsSegs.get(0);

		// Find the exon that contains the last CDS segment's start — this is the
		// translation end_Exon (Perl BaseGXF.pm line 568: $_->{_exon} = $exon).
		// Then compute the offset of the CDS end relative to that exon's start
		// (Perl line 723-728), which may exceed the exon length.
		int cdnaPos = 0;
		if (transcript.isPositiveStrand()) {
			for (ExonModel exon : transcript.getExons()) {
				if (lastCds.getStart() >= exon.getStart() && lastCds.getStart() <= exon.getEnd()) {
					// This is the end exon. Perl: $end += $self->translation->end
					// translation->end = (cds_end - exon_start) + 1
					int translationEnd = (lastCds.getEnd() - exon.getStart()) + 1;
					return cdnaPos + translationEnd;
				}
				cdnaPos += exon.getEnd() - exon.getStart() + 1;
			}
		} else {
			for (int i = transcript.getExons().size() - 1; i >= 0; i--) {
				ExonModel exon = transcript.getExons().get(i);
				if (lastCds.getEnd() >= exon.getStart() && lastCds.getEnd() <= exon.getEnd()) {
					// Minus strand: translation->end = (exon_end - cds_start) + 1
					int translationEnd = (exon.getEnd() - lastCds.getStart()) + 1;
					return cdnaPos + translationEnd;
				}
				cdnaPos += exon.getEnd() - exon.getStart() + 1;
			}
		}
		return 0;
	}

	private int compareHgvsPositions(String pos1, String pos2) {
		if (pos1 == null || pos2 == null) return 0;
		boolean star1 = pos1.startsWith("*");
		boolean star2 = pos2.startsWith("*");
		if (star1 && !star2) return 1;
		if (!star1 && star2) return -1;
		String p1 = star1 ? pos1.substring(1) : pos1;
		String p2 = star2 ? pos2.substring(1) : pos2;
		int[] parsed1 = parseHgvsPosition(p1);
		int[] parsed2 = parseHgvsPosition(p2);
		int cmp = Integer.compare(parsed1[0], parsed2[0]);
		return cmp != 0 ? cmp : Integer.compare(parsed1[1], parsed2[1]);
	}

	private int parseHgvsPos(String pos) {
		if (pos == null || pos.isEmpty()) return 0;
		try { return parseHgvsPosition(pos)[0]; }
		catch (Exception e) { return 0; }
	}

	private int[] parseHgvsPosition(String pos) {
		int plusIdx = pos.indexOf('+');
		int minusIdx = pos.lastIndexOf('-');
		if (minusIdx == 0) minusIdx = -1;
		if (plusIdx > 0) {
			return new int[]{Integer.parseInt(pos.substring(0, plusIdx)),
				Integer.parseInt(pos.substring(plusIdx + 1))};
		} else if (minusIdx > 0) {
			return new int[]{Integer.parseInt(pos.substring(0, minusIdx)),
				-Integer.parseInt(pos.substring(minusIdx + 1))};
		}
		return new int[]{Integer.parseInt(pos), 0};
	}
}
