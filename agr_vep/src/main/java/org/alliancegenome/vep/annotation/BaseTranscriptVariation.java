package org.alliancegenome.vep.annotation;

import org.alliancegenome.vep.model.TranscriptModel;

/**
 * Port of Bio::EnsEMBL::Variation::BaseTranscriptVariation.
 * Holds coordinate mappings for a variant-transcript overlap.
 *
 * VEP uses TranscriptMapper with three calls:
 *   genomic2cdna → cdna_start/end (line 488)
 *   genomic2cds  → cds_start/end + exon_phase (line 513, 263)
 *   genomic2pep  → translation_start/end (line 548)
 *
 * For insertions, the mapper can return start > end (between-codon).
 * codon_position comes from TranscriptVariation.pm line 287-307.
 */
public class BaseTranscriptVariation {

	private final TranscriptModel transcript;
	private final int genomicStart;
	private final int genomicEnd;

	// VEP BaseTranscriptVariation fields
	private int cdnaStart;
	private int cdnaEnd;
	private int cdsStart;
	private int cdsEnd;
	private int translationStart;
	private int translationEnd;
	private int codonPosition;

	public BaseTranscriptVariation(TranscriptModel transcript, int genomicStart, int genomicEnd,
			TranscriptVariationAllele tva) {
		this.transcript = transcript;
		this.genomicStart = genomicStart;
		this.genomicEnd = genomicEnd;
		compute(tva);
	}

	private void compute(TranscriptVariationAllele tva) {
		// Map genomic positions to CDS
		// VEP genomic2cds returns Coordinate objects; first.start and last.end
		int cdsA = tva.genomicToCdsPosition(transcript, genomicStart);
		int cdsB = tva.genomicToCdsPosition(transcript, genomicEnd);

		// Handle unmapped positions
		if (cdsA < 0 && cdsB < 0) {
			this.cdsStart = -1;
			this.cdsEnd = -1;
			return;
		}
		if (cdsA < 0) cdsA = cdsB;
		if (cdsB < 0) cdsB = cdsA;

		// VEP cds_start/end (line 258-264):
		// first.start = lower mapped position, last.end = higher mapped position
		// Plus exon_phase offset (line 261-263)
		int exonPhase = transcript.getStartExonPhase();
		int phaseOffset = exonPhase > 0 ? exonPhase : 0;
		this.cdsStart = Math.min(cdsA, cdsB) + phaseOffset;
		this.cdsEnd = Math.max(cdsA, cdsB) + phaseOffset;

		// VEP cdna_start/end (line 143-148):
		// From genomic2cdna mapper: first.start, last.end
		int cdnaCodingStart = transcript.getCdnaCodingStart();
		if (cdnaCodingStart <= 0) cdnaCodingStart = 1;
		this.cdnaStart = this.cdsStart + (cdnaCodingStart - 1) - phaseOffset;
		this.cdnaEnd = this.cdsEnd + (cdnaCodingStart - 1) - phaseOffset;

		// VEP translation_start/end (line 365-377):
		// From genomic2pep mapper: first.start, last.end
		// The mapper can return start > end for between-codon insertions.
		// We approximate by mapping each CDS endpoint to protein independently.
		int pepA = (cdsA + phaseOffset - 1) / 3 + 1;
		int pepB = (cdsB + phaseOffset - 1) / 3 + 1;
		// VEP: first.start and last.end from the mapper result list
		// For insertions between codons: pepA and pepB map to adjacent protein positions
		// The mapper preserves the genomic order, so for minus strand:
		//   genomicStart (higher genomic) → lower CDS → lower protein
		//   genomicEnd (lower genomic) → higher CDS → higher protein
		// VEP's first/last ordering depends on the mapper internals.
		// For now, match VEP by keeping the order from the genomic mapping:
		//   translation_start = pep from genomicStart's CDS
		//   translation_end = pep from genomicEnd's CDS
		int pepFromStart = (tva.genomicToCdsPosition(transcript, genomicStart) + phaseOffset);
		int pepFromEnd = (tva.genomicToCdsPosition(transcript, genomicEnd) + phaseOffset);
		if (pepFromStart > 0) pepFromStart = (pepFromStart - 1) / 3 + 1;
		if (pepFromEnd > 0) pepFromEnd = (pepFromEnd - 1) / 3 + 1;

		if (pepFromStart > 0 && pepFromEnd > 0) {
			this.translationStart = pepFromStart;
			this.translationEnd = pepFromEnd;
		} else if (pepFromStart > 0) {
			this.translationStart = pepFromStart;
			this.translationEnd = pepFromStart;
		} else if (pepFromEnd > 0) {
			this.translationStart = pepFromEnd;
			this.translationEnd = pepFromEnd;
		}

		// VEP codon_position (TranscriptVariation.pm line 287-307):
		// ((cdna_start - tran_cdna_start + phase_offset) % 3) + 1
		if (this.cdnaStart > 0 && cdnaCodingStart > 0) {
			this.codonPosition = ((this.cdnaStart - cdnaCodingStart + phaseOffset) % 3) + 1;
		}
	}

	public int cdnaStart() { return cdnaStart; }
	public int cdnaEnd() { return cdnaEnd; }
	public int cdsStart() { return cdsStart; }
	public int cdsEnd() { return cdsEnd; }
	public int translationStart() { return translationStart; }
	public int translationEnd() { return translationEnd; }
	public int codonPosition() { return codonPosition; }
	public TranscriptModel transcript() { return transcript; }
}
