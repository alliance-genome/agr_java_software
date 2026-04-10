package org.alliancegenome.vep.annotation;

import java.util.List;

import org.alliancegenome.vep.model.Mapper;
import org.alliancegenome.vep.model.TranscriptMapper;
import org.alliancegenome.vep.model.TranscriptModel;

/**
 * Port of Bio::EnsEMBL::Variation::BaseTranscriptVariation.
 * Uses TranscriptMapper → Mapper for all coordinate conversions.
 */
public class BaseTranscriptVariation {

	private final TranscriptModel transcript;
	private final int genomicStart;
	private final int genomicEnd;

	private int cdnaStart = -1;
	private int cdnaEnd = -1;
	private int cdsStart = -1;
	private int cdsEnd = -1;
	private int translationStart = -1;
	private int translationEnd = -1;
	private int codonPosition;

	public BaseTranscriptVariation(TranscriptModel transcript, int genomicStart, int genomicEnd,
			TranscriptVariationAllele tva) {
		this.transcript = transcript;
		this.genomicStart = genomicStart;
		this.genomicEnd = genomicEnd;
		compute();
	}

	private void compute() {
		TranscriptMapper mapper = new TranscriptMapper(transcript);
		int strand = transcript.isPositiveStrand() ? 1 : -1;

		// VEP cdna_start/end (BaseTranscriptVariation.pm line 143-148)
		List<Mapper.Result> cdnaCoords = mapper.genomic2cdna(genomicStart, genomicEnd, strand);
		Mapper.Result cdnaFirst = cdnaCoords.get(0);
		Mapper.Result cdnaLast = cdnaCoords.get(cdnaCoords.size() - 1);
		this.cdnaStart = cdnaFirst.isGap() ? -1 : cdnaFirst.coordinate.start;
		this.cdnaEnd = cdnaLast.isGap() ? -1 : cdnaLast.coordinate.end;

		// VEP cds_start/end (BaseTranscriptVariation.pm line 258-264)
		List<Mapper.Result> cdsCoords = mapper.genomic2cds(genomicStart, genomicEnd, strand);
		Mapper.Result cdsFirst = cdsCoords.get(0);
		Mapper.Result cdsLast = cdsCoords.get(cdsCoords.size() - 1);
		int exonPhase = transcript.getStartExonPhase();
		int phaseOffset = exonPhase > 0 ? exonPhase : 0;
		this.cdsStart = cdsFirst.isGap() ? -1 : cdsFirst.coordinate.start + phaseOffset;
		this.cdsEnd = cdsLast.isGap() ? -1 : cdsLast.coordinate.end + phaseOffset;

		if (this.cdsStart < 0 && this.cdsEnd < 0) return;

		// VEP translation_start/end (BaseTranscriptVariation.pm line 371-376)
		List<Mapper.Result> pepCoords = mapper.genomic2pep(genomicStart, genomicEnd, strand);
		Mapper.Result pepFirst = pepCoords.get(0);
		Mapper.Result pepLast = pepCoords.get(pepCoords.size() - 1);
		this.translationStart = pepFirst.isGap() ? -1 : pepFirst.coordinate.start;
		this.translationEnd = pepLast.isGap() ? -1 : pepLast.coordinate.end;

		// VEP codon_position (TranscriptVariation.pm line 287-307)
		int cdnaCodingStart = mapper.getCdnaCodingStart();
		if (cdnaCodingStart <= 0) cdnaCodingStart = 1;
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
