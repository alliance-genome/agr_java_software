package org.alliancegenome.vep.hgvs;

import java.util.List;

import org.alliancegenome.vep.bio.Sequence;
import org.alliancegenome.vep.model.CdsSegment;
import org.alliancegenome.vep.model.ExonModel;
import org.alliancegenome.vep.model.TranscriptModel;
import org.alliancegenome.vep.reference.ReferenceGenome;

import lombok.extern.log4j.Log4j2;

/**
 * Generates HGVSc (coding) notation matching VEP's hgvs_transcript().
 *
 * VEP flow (TranscriptVariationAllele.pm lines 1294-1486):
 * 1. Both start/end positions mapped through _get_cDNA_position() independently
 * 2. Positions swapped if needed to ensure ascending order
 * 3. Variant type (del/ins/dup/>/delins) determined by _clip_alleles
 * 4. format_hgvs_string() assembles final notation
 */
@Log4j2
public class HgvsCodingNotation {

	private final ReferenceGenome reference;

	public HgvsCodingNotation() {
		this(null);
	}

	public HgvsCodingNotation(ReferenceGenome reference) {
		this.reference = reference;
	}

	public String generate(TranscriptModel transcript, String chr, int variantStart, int variantEnd,
			String vepAllele, String refAllele, int cdsPosition, boolean isCoding) {

		String prefix = isCoding ? "c." : "n.";
		String transcriptRef = transcript.getTranscriptId();
		if (transcriptRef == null) return null;

		// VEP line 1425-1426: append .version() unless ID already ends with .\d+
		if (!transcriptRef.matches(".*\\.\\d+$")) {
			transcriptRef = transcriptRef + ".1";
		}

		// VEP maps BOTH positions through _get_cDNA_position() independently
		// (TranscriptVariationAllele.pm line 1445-1446)
		String startPos = getCdnaPosition(transcript, variantStart, isCoding);
		String endPos = getCdnaPosition(transcript, variantEnd, isCoding);

		log.debug("HGVSc generate: transcript={} varStart={} varEnd={} allele={}/{} cdsPos={} isCoding={} startPos={} endPos={}",
			transcript.getTranscriptId(), variantStart, variantEnd, refAllele, vepAllele, cdsPosition, isCoding, startPos, endPos);

		if (startPos == null && endPos == null) return null;
		if (startPos == null) startPos = endPos;
		if (endPos == null) endPos = startPos;

		// VEP line 1456-1459: ensure ascending order
		// Parse exon coord and intron offset from each position string
		if (compareHgvsPositions(startPos, endPos) > 0) {
			String tmp = startPos;
			startPos = endPos;
			endPos = tmp;
			log.debug("HGVSc: swapped positions: {} _ {}", startPos, endPos);
		}

		// Get strand-aware ref/alt
		String hgvsRef = transcript.isPositiveStrand() ? refAllele : Sequence.reverseComplement(refAllele);
		String hgvsAlt = transcript.isPositiveStrand() ? vepAllele : Sequence.reverseComplement(vepAllele);

		// Determine variant type and format notation
		String notation;
		if ("-".equals(vepAllele)) {
			// Deletion
			if (startPos.equals(endPos)) {
				notation = startPos + "del";
			} else {
				notation = startPos + "_" + endPos + "del";
			}
		} else if ("-".equals(refAllele)) {
			// Insertion: check for duplication (VEP Sequence.pm line 570-588)
			// VEP compares on the transcript SLICE. The slice is reverse-complemented for
			// minus strand. dup_lookup_direction: -1=backward (+ strand), +1=forward (- strand).
			// In both cases this checks the PRECEDING genomic bases (lower positions):
			//	 +strand: backward in slice = backward in genome = preceding
			//	 -strand: forward in slice = backward in genome = preceding
			// Compare the GENOMIC vepAllele against preceding GENOMIC reference bases.
			boolean isDup = false;
			int altLen = vepAllele.length();
			if (reference != null && chr != null) {
				try {
					// VEP Sequence.pm line 573-575: check preceding bases on the slice.
					// For both strands, this corresponds to the lower insertion boundary
					// and the bases before it in the genomic reference.
					int refEnd = Math.min(variantStart, variantEnd);
					int refStart = refEnd - altLen + 1;
					if (refStart >= 1) {
						String preceding = reference.getSequence(chr, refStart, refEnd);
						isDup = vepAllele.equalsIgnoreCase(preceding);
					}
				} catch (Exception e) {
					// ignore
				}
			}
			if (isDup) {
				// Dup notation: position of the duplicated bases
				// VEP line 581: start = display_end - alt_length + 1 (shifts back)
				// The duplicated region ends at the lower insertion boundary
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
			// SNP
			notation = startPos + hgvsRef + ">" + hgvsAlt;
		} else {
			// VEP hgvs_transcript line 1418: _clip_alleles unless type is 'dup'
			// Uses TranscriptVariationAllele._clip_alleles (line 2102) which does
			// BOTH prefix AND suffix trimming, adjusting start/end.
			// Parse startPos/endPos to integers for clipping
			int clipStartInt = parseHgvsPos(startPos);
			int clipEndInt = parseHgvsPos(endPos);
			if (clipEndInt == 0) clipEndInt = clipStartInt;

			org.alliancegenome.vep.annotation.TranscriptVariationAllele.HgvsNotation clipped =
				org.alliancegenome.vep.annotation.TranscriptVariationAllele.vepClipAlleles(
					hgvsRef, hgvsAlt, clipStartInt, clipEndInt);

			// Recompute cDNA positions from the clipped genomic positions
			// VEP _clip_alleles adjusts start/end integers directly
			if (clipped.preseq != null && !clipped.preseq.isEmpty()) {
				// Prefix was clipped — recompute start position
				int prefixLen = clipped.preseq.length();
				int newStartGenomic = transcript.isPositiveStrand()
					? variantStart + prefixLen : variantEnd - prefixLen;
				startPos = getCdnaPosition(transcript, newStartGenomic, isCoding);
				if (startPos == null) startPos = String.valueOf(clipped.start);
			}

			String clippedRef = clipped.ref;
			String clippedAlt = clipped.alt;

			// Format using Sequence.formatHgvsString-style logic
			if ("=".equals(clipped.type)) {
				notation = startPos + "=";
			} else if ("ins".equals(clipped.type) || (clippedRef.isEmpty() && !clippedAlt.isEmpty())) {
				// For insertion after prefix clip: end_start (swapped)
				notation = endPos + "_" + startPos + "ins" + clippedAlt;
			} else if ("del".equals(clipped.type) || (!clippedRef.isEmpty() && clippedAlt.isEmpty())) {
				if (startPos.equals(endPos)) {
					notation = startPos + "del";
				} else {
					notation = startPos + "_" + endPos + "del";
				}
			} else if (">".equals(clipped.type) || (clippedRef.length() == 1 && clippedAlt.length() == 1)) {
				notation = startPos + clippedRef + ">" + clippedAlt;
			} else if ("dup".equals(clipped.type)) {
				if (startPos.equals(endPos)) {
					notation = startPos + "dup";
				} else {
					notation = startPos + "_" + endPos + "dup";
				}
			} else {
				// delins
				if (startPos.equals(endPos)) {
					notation = startPos + "delins" + clippedAlt;
				} else {
					notation = startPos + "_" + endPos + "delins" + clippedAlt;
				}
			}
		}

		String result = transcriptRef + ":" + prefix + notation;
		log.debug("HGVSc result: {}", result);
		return result;
	}

	/**
	 * VEP _get_cDNA_position (TranscriptVariationAllele.pm line 2662-2784):
	 * Converts a genomic position to a cDNA-relative HGVS position string.
	 * Returns strings like "123", "123+45", "123-45", "*37", "-45", "*37+10"
	 */
	String getCdnaPosition(TranscriptModel transcript, int genomicPos, boolean isCoding) {
		List<ExonModel> exons = transcript.getExons();
		boolean positiveStrand = transcript.isPositiveStrand();
		int strand = positiveStrand ? 1 : -1;

		// VEP _get_cDNA_position line 2684-2738:
		// Iterates exons in GENOMIC forward order regardless of strand.
		// Pre-compute cDNA start/end for each exon matching VEP's
		// _exon_cdna_start/_exon_cdna_end (lines 2788-2812).
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
			// Minus strand: cDNA runs from highest genomic exon to lowest
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

		// VEP line 2684: loop exons in genomic forward order
		for (int i = 0; i < exons.size(); i++) {
			ExonModel exon = exons.get(i);

			// Skip if position is beyond this exon
			if (genomicPos > exon.getEnd()) continue;

			// EXONIC (line 2694): position within this exon
			if (genomicPos >= exon.getStart()) {
				cdnaPosition = exCdnaStart[i] + (
					positiveStrand ?
					(genomicPos - exon.getStart()) :
					(exon.getEnd() - genomicPos)
				);
				break;
			}

			// INTRONIC (line 2707): position between prev exon and this exon
			if (i > 0) {
				ExonModel prevExon = exons.get(i - 1);
				int updist = Math.abs(genomicPos - prevExon.getEnd());
				int downdist = Math.abs(exon.getStart() - genomicPos);

				log.debug("getCdnaPosition: INTRONIC genomicPos={} strand={} prevExon={}-{} thisExon={}-{} updist={} downdist={} prevCdna=[{},{}] thisCdna=[{},{}]",
					genomicPos, strand, prevExon.getStart(), prevExon.getEnd(),
					exon.getStart(), exon.getEnd(), updist, downdist,
					exCdnaStart[i-1], exCdnaEnd[i-1], exCdnaStart[i], exCdnaEnd[i]);

				// VEP line 2717-2734
				if (updist < downdist || (updist == downdist && strand >= 0)) {
					// Closer to prev (upstream) exon
					// +strand: cdna_end(prev) + "+" (line 2722)
					// -strand: cdna_start(prev) + "-" (line 2723)
					cdnaPosition = positiveStrand ? exCdnaEnd[i-1] : exCdnaStart[i-1];
					intronOffset = (positiveStrand ? "+" : "-") + updist;
				} else {
					// Closer to current (downstream) exon
					// +strand: cdna_start(exon) + "-" (line 2731)
					// -strand: cdna_end(exon) + "+" (line 2732)
					cdnaPosition = positiveStrand ? exCdnaStart[i] : exCdnaEnd[i];
					intronOffset = (positiveStrand ? "-" : "+") + downdist;
				}
				break;
			}
			break;
		}

		if (cdnaPosition == null) {
			log.debug("getCdnaPosition: genomicPos={} strand={} → null (not in any exon/intron)",
				genomicPos, positiveStrand ? "+" : "-");
			return null;
		}

		log.debug("getCdnaPosition: genomicPos={} strand={} → raw cdnaPos={} intronOffset={}",
			genomicPos, positiveStrand ? "+" : "-", cdnaPosition, intronOffset);

		// VEP line 2744-2781: adjust for coding region boundaries (start/stop codon)
		if (isCoding) {
			int cdnaCodingStart = computeCdnaCodingStart(transcript);
			int cdnaCodingEnd = computeCdnaCodingEnd(transcript);

			log.debug("getCdnaPosition: coding adjustment: cdnaCodingStart={} cdnaCodingEnd={} cdnaPos={}",
				cdnaCodingStart, cdnaCodingEnd, cdnaPosition);

			if (cdnaCodingEnd > 0 && cdnaPosition > cdnaCodingEnd) {
				cdnaPosition -= cdnaCodingEnd;
				String result = "*" + cdnaPosition + (intronOffset != null ? intronOffset : "");
				log.debug("getCdnaPosition: 3'UTR → {}", result);
				return result;
			}

			if (cdnaCodingEnd > 0 && cdnaPosition == cdnaCodingEnd && intronOffset != null) {
				String cleanOffset = intronOffset.replace("+", "");
				String result = "*" + cleanOffset;
				log.debug("getCdnaPosition: 3'UTR boundary → {}", result);
				return result;
			}

			if (cdnaCodingStart > 0) {
				if (cdnaPosition >= cdnaCodingStart) {
					cdnaPosition++;
				}
				cdnaPosition -= cdnaCodingStart;
			}
		}

		String result = cdnaPosition + (intronOffset != null ? intronOffset : "");
		log.debug("getCdnaPosition: final → {}", result);
		return result;
	}

	/**
	 * Compare two HGVS position strings for ordering.
	 * VEP line 1451-1459: parse exon coord and intron offset, compare.
	 */
	private int compareHgvsPositions(String pos1, String pos2) {
		if (pos1 == null || pos2 == null) return 0;

		// Handle 3'UTR positions (start with *)
		boolean star1 = pos1.startsWith("*");
		boolean star2 = pos2.startsWith("*");
		if (star1 && !star2) return 1;	// * positions are always after non-*
		if (!star1 && star2) return -1;

		String p1 = star1 ? pos1.substring(1) : pos1;
		String p2 = star2 ? pos2.substring(1) : pos2;

		// Parse exon coordinate and intron offset
		int[] parsed1 = parseHgvsPosition(p1);
		int[] parsed2 = parseHgvsPosition(p2);

		int cmp = Integer.compare(parsed1[0], parsed2[0]);
		if (cmp != 0) return cmp;
		return Integer.compare(parsed1[1], parsed2[1]);
	}

	/**
	 * Parse a position string like "567+195" or "567-45" or "567" into [exonCoord, intronOffset].
	 */
	/** Extract the base integer from an HGVS position string (strips +/- offset). */
	private int parseHgvsPos(String pos) {
		if (pos == null || pos.isEmpty()) return 0;
		try {
			return parseHgvsPosition(pos)[0];
		} catch (Exception e) {
			return 0;
		}
	}

	private int[] parseHgvsPosition(String pos) {
		int exonCoord = 0;
		int intronOffset = 0;

		int plusIdx = pos.indexOf('+');
		int minusIdx = pos.lastIndexOf('-');
		// Avoid matching leading minus (negative exon coord like "-45")
		if (minusIdx == 0) minusIdx = -1;

		if (plusIdx > 0) {
			exonCoord = Integer.parseInt(pos.substring(0, plusIdx));
			intronOffset = Integer.parseInt(pos.substring(plusIdx + 1));
		} else if (minusIdx > 0) {
			exonCoord = Integer.parseInt(pos.substring(0, minusIdx));
			intronOffset = -Integer.parseInt(pos.substring(minusIdx + 1));
		} else {
			exonCoord = Integer.parseInt(pos);
		}
		return new int[]{exonCoord, intronOffset};
	}

	/**
	 * Compute cDNA position of the CDS start (first base of start codon).
	 * Equivalent to VEP's $transcript->cdna_coding_start().
	 */
	private int computeCdnaCodingStart(TranscriptModel transcript) {
		if (!transcript.isCoding()) return 0;

		int cdsGenomicStart = transcript.isPositiveStrand()
			? transcript.getCdsStart() : transcript.getCdsEnd();

		int cdnaPos = 0;
		if (transcript.isPositiveStrand()) {
			for (ExonModel exon : transcript.getExons()) {
				if (cdsGenomicStart >= exon.getStart() && cdsGenomicStart <= exon.getEnd()) {
					return cdnaPos + (cdsGenomicStart - exon.getStart()) + 1;
				}
				cdnaPos += exon.getEnd() - exon.getStart() + 1;
			}
		} else {
			for (int i = transcript.getExons().size() - 1; i >= 0; i--) {
				ExonModel exon = transcript.getExons().get(i);
				if (cdsGenomicStart >= exon.getStart() && cdsGenomicStart <= exon.getEnd()) {
					return cdnaPos + (exon.getEnd() - cdsGenomicStart) + 1;
				}
				cdnaPos += exon.getEnd() - exon.getStart() + 1;
			}
		}
		return 0;
	}

	/**
	 * Compute cDNA position of the CDS end (last base of stop codon).
	 * Equivalent to VEP's $transcript->cdna_coding_end().
	 */
	private int computeCdnaCodingEnd(TranscriptModel transcript) {
		if (!transcript.isCoding()) return 0;

		int cdsGenomicEnd = transcript.isPositiveStrand()
			? transcript.getCdsEnd() : transcript.getCdsStart();

		int cdnaPos = 0;
		if (transcript.isPositiveStrand()) {
			for (ExonModel exon : transcript.getExons()) {
				if (cdsGenomicEnd >= exon.getStart() && cdsGenomicEnd <= exon.getEnd()) {
					return cdnaPos + (cdsGenomicEnd - exon.getStart()) + 1;
				}
				cdnaPos += exon.getEnd() - exon.getStart() + 1;
			}
		} else {
			for (int i = transcript.getExons().size() - 1; i >= 0; i--) {
				ExonModel exon = transcript.getExons().get(i);
				if (cdsGenomicEnd >= exon.getStart() && cdsGenomicEnd <= exon.getEnd()) {
					return cdnaPos + (exon.getEnd() - cdsGenomicEnd) + 1;
				}
				cdnaPos += exon.getEnd() - exon.getStart() + 1;
			}
		}
		return 0;
	}
}
