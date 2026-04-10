package org.alliancegenome.vep.annotation;

import org.alliancegenome.vep.model.TranscriptModel;

/**
 * Port of Bio::EnsEMBL::Variation::BaseTranscriptVariation.
 * Holds coordinate mappings for a variant-transcript overlap:
 * cdna_start/end, cds_start/end, translation_start/end, codon_position.
 *
 * VEP computes these via Mapper (genomic → cDNA → CDS → protein).
 * Our implementation computes from the TranscriptModel directly.
 */
public class BaseTranscriptVariation {

	private final TranscriptModel transcript;
	private final int genomicStart;
	private final int genomicEnd;

	// VEP BaseTranscriptVariation fields
	private int cdnaStart;   // lower cDNA position
	private int cdnaEnd;     // higher cDNA position
	private int cdsStart;    // lower CDS position (+ exon_phase offset)
	private int cdsEnd;      // higher CDS position (+ exon_phase offset)
	private int translationStart; // protein position from cds_start
	private int translationEnd;   // protein position from cds_end
	private int codonPosition;    // 1-based position within codon (TranscriptVariation.pm line 287)

	public BaseTranscriptVariation(TranscriptModel transcript, int genomicStart, int genomicEnd,
			TranscriptVariationAllele tva) {
		this.transcript = transcript;
		this.genomicStart = genomicStart;
		this.genomicEnd = genomicEnd;
		computeCoordinates(tva);
	}

	private void computeCoordinates(TranscriptVariationAllele tva) {
		// Map genomic → CDS using the existing genomicToCdsPosition
		int cdsA = tva.genomicToCdsPosition(transcript, genomicStart);
		int cdsB = tva.genomicToCdsPosition(transcript, genomicEnd);

		// VEP cds_start/end (line 252-268): first.start, last.end from mapper
		// The mapper returns coordinates in order: first = lower, last = higher
		// Plus exon_phase offset (line 263)
		int exonPhase = transcript.getStartExonPhase();
		int phaseOffset = exonPhase > 0 ? exonPhase : 0;

		if (cdsA >= 0 && cdsB >= 0) {
			this.cdsStart = Math.min(cdsA, cdsB) + phaseOffset;
			this.cdsEnd = Math.max(cdsA, cdsB) + phaseOffset;
		} else if (cdsA >= 0) {
			this.cdsStart = cdsA + phaseOffset;
			this.cdsEnd = cdsA + phaseOffset;
		} else if (cdsB >= 0) {
			this.cdsStart = cdsB + phaseOffset;
			this.cdsEnd = cdsB + phaseOffset;
		} else {
			this.cdsStart = -1;
			this.cdsEnd = -1;
		}

		// VEP cdna_start/end (line 143-148): from cdna_coords mapper
		// cdna_start = lower cDNA position (always)
		// For our purposes: cdna = cds + UTR offset
		int cdnaCodingStart = transcript.getCdnaCodingStart();
		if (cdnaCodingStart <= 0) cdnaCodingStart = 1;
		int utrOffset = cdnaCodingStart - 1;
		if (cdsStart > 0) {
			this.cdnaStart = cdsStart + utrOffset - phaseOffset; // remove phase, add UTR
			this.cdnaEnd = cdsEnd + utrOffset - phaseOffset;
		}

		// VEP translation_start/end (line 351-398): ceil(cds_pos / 3)
		if (cdsStart > 0) {
			this.translationStart = (cdsStart - 1) / 3 + 1;
			this.translationEnd = (cdsEnd - 1) / 3 + 1;
		}

		// VEP codon_position (TranscriptVariation.pm line 287-307):
		// ((cdna_start - tran_cdna_start + phase_offset) % 3) + 1
		if (cdnaStart > 0 && cdnaCodingStart > 0) {
			this.codonPosition = ((cdnaStart - cdnaCodingStart + phaseOffset) % 3) + 1;
		}
	}

	// Getters matching VEP method names
	public int cdnaStart() { return cdnaStart; }
	public int cdnaEnd() { return cdnaEnd; }
	public int cdsStart() { return cdsStart; }
	public int cdsEnd() { return cdsEnd; }
	public int translationStart() { return translationStart; }
	public int translationEnd() { return translationEnd; }
	public int codonPosition() { return codonPosition; }
	public TranscriptModel transcript() { return transcript; }
}
